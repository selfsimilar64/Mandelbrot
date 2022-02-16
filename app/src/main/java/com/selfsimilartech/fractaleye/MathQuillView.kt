package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.WebView
import android.widget.ImageView
import java.util.*

enum class Keystroke(val str: String) { LEFT("Left"), RIGHT("Right"), BACKSPACE("Backspace") }

class MathQuillView : WebView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    lateinit var fsv : FractalSurfaceView
    lateinit var eqnErrorIndicator : ImageView

    fun enterExpr(expr: Expr) {

        if (expr.isCmd) evaluateJavascript("javascript:enterCmd('${expr.latex}')", null)
        else evaluateJavascript("javascript:enterTypedText('${expr.latex}')", null)

        if (expr in Expr.logarithmic) enterKeystroke(Keystroke.RIGHT)

        if (expr.insertParens) evaluateJavascript("javascript:mathField.typedText('()')", null)

        when {
            expr.rightAfter -> enterKeystroke(Keystroke.RIGHT)
            expr.leftAfter -> enterKeystroke(Keystroke.LEFT)
        }

    }

    fun enterKeystroke(ks: Keystroke) {
        evaluateJavascript("javascript:mathField.keystroke('${ks.str}')", null)
    }

    fun getLatex(shape: Shape) {
        evaluateJavascript("javascript:mathField.latex()") {
            Log.d("MQ", "result: $it")
            setCustomLoop(it.substring(1..it.length-2), shape)
        }
    }

    fun setLatex(latex: String) {
        evaluateJavascript("javascript:mathField.latex('$latex')", null)
    }

    fun setCustomLoop(latex: String, shape: Shape) {
        val parsed = parseEquation(latex)
        if (parsed == null) eqnErrorIndicator.show()
        else {
            val postfixSingle = infixToPostfix(parsed)
            val postfixDual = ArrayList(postfixSingle.map {
                if (it is Operator || it.isConstant) it
                else Expr(it).apply { precision = Precision.DUAL }
            })
            val sf = postfixToGlsl(postfixSingle)
            val df = postfixToGlsl(postfixDual)
            Log.d("SHAPE", "customLoopSF: ${sf?.str}")
            Log.d("SHAPE", "customLoopDF: ${df?.str}")
            if (sf == null || df == null) {
                eqnErrorIndicator.show()
            } else {
                eqnErrorIndicator.makeInvisible()
                shape.latex = latex
                shape.customLoopSingle = sf.str
                shape.customLoopDual = df.str
                shape.slowDualFloat = sf.zoomLevel == ZoomLevel.SHALLOW
                fsv.r.renderShaderChanged = true
                fsv.r.renderToTex = true
                fsv.requestRender()
            }
        }
    }

    private fun parseEquation(input: String) : ArrayList<Expr>? {

        val out = arrayListOf<Expr>()
        var str = input


        // initial processing
        str = str.replace("\\\\", "")       // remove all \\
                .replace("left|", "modulus(")
                .replace("right|", ")")
                .replace("left", "")      // remove all left
                .replace("right", "")     // remove all right
                .replace(Regex("operatorname\\{[A-Za-z]+\\}")) { result -> result.value.substring(13 until result.value.length - 1) }


        // replace all \\frac{}{} with ({}/{})
        do {
            val start = str.indexOf("frac")
            Log.e("SHAPE", "str: $str")
            Log.e("SHAPE", "start: $start")
            if (start != -1) {

                var left = 0
                var right = 0
                var middle = 0
                var end = 0
                var middleFound = false
                var endFound = false

                var i = start + 5
                while (i < str.length) {
                    when (str[i]) {
                        '}' -> {
                            if (left == right) {
                                if (!middleFound) {
                                    middle = i
                                    middleFound = true
                                    i++
                                }
                                else {
                                    end = i
                                    endFound = true
                                }
                            }
                            else right += 1
                        }
                        '{' -> left += 1
                    }
                    if (endFound) {
                        str = str
                                .replaceRange( end,    end    + 1, "})"  )
                                .replaceRange( middle, middle + 2, "}/{" )
                                .replaceRange( start,  start  + 4, "("   )
                        break
                    }
                    i++
                }
            }
            else break
        } while (str.contains("frac"))


        // replace all \\cdot with *
        while (true) {
            val start = str.indexOf("cdot")
            if (start != -1) {
                str = str.replaceRange(start, start + 4, "*")
            } else break
        }


        str = str
                .replace(" ", "")
                .replace("{", "(")
                .replace("}", ")")


        // parse string into expressions
        while (str.isNotEmpty()) {
            var end = minOf(str.length, Expr.MAX_EXPR_LEN)
            while (end > 0) {
                val expr = Expr.valueOf(str.substring(0 until end))
                if (expr != null) {
                    // Log.e("SHAPE", "expr found: ${str.substring(0 until end)}")
                    out.add(expr)
                    str = str.removeRange(0 until end)
                    break
                }
                else {
                    if (end == 1) {
                        Log.e("SHAPE", "no matching expression: $str")
                        return null
                    } else end--
                }
            }
        }


        // implicit negative
        out.forEachIndexed { i, expr ->
            if (expr == Expr.sub && (i == 0 || out[i - 1] in listOf(Expr.add, Expr.sub, Expr.mult, Expr.div, Expr.leftParen))) {
                // Log.d("SHAPE", "prev expr: ${out[i - 1]}")
                out[i] = Expr.neg
            }
        }

        // implicit multiply
        var i = 1
        var outSize = out.size
        while (i < outSize) {
            if (out[i - 1] !is Operator && (out[i] == Expr.i || !out[i].isConstant)) {
                if (out[i] !is Operator || out[i].run { this is Operator && numArgs == 1 }) {
                    if (!(out[i - 1] == Expr.leftParen || out[i] == Expr.rightParen)) {
                        // Log.d("SHAPE", "implicit multiply: ${out[i - 1]} -> ${out[i]}")
                        out.add(i, Expr.mult)
                        outSize++
                    }
                }
            }
            i++
        }

        // Log.d("SHAPE", "parsed equation: ${out.joinToString(" ")}")
        // Log.e("SHAPE", "out size: ${out.size}")
        return out

    }

    private fun infixToPrefix(exprs: ArrayList<Expr>) : ArrayList<Expr> {

        // val expr = input.reversed()
        // z*(2.0*z - 1.0) + (c - 1.0)^2.0
        exprs.reverse()
        exprs.forEachIndexed { i, expr ->
            exprs[i] = when (expr) {
                Expr.leftParen -> Expr.rightParen
                Expr.rightParen -> Expr.leftParen
                else -> expr
            }
        }
        exprs.add(Expr.rightParen)
        val stack = Stack<Expr>().apply { push(Expr.leftParen) }
        val out = arrayListOf<Expr>()

        while (stack.isNotEmpty()) {

            //Log.e("SHAPE", "expr: " + exprs.joinToString(separator = ""))
            //Log.e("SHAPE", "stack: " + stack.joinToString(separator = " "))
            //Log.e("SHAPE", "out: " + out.joinToString(separator = " "))

            when (val element = exprs.removeAt(0)) {
                is Operator -> {
                    while (stack.peek() != Expr.leftParen && (stack.peek() as Operator).order <= element.order) {
                        out.add(stack.pop())
                    }
                    stack.add(element)
                }
                Expr.leftParen -> stack.push(element)
                Expr.rightParen -> {
                    while (stack.peek() != Expr.leftParen) out.add(stack.pop())
                    stack.pop()
                }
                else -> out.add(element)  // operand
            }
        }

        // Log.e("SHAPE", "out: " + out.joinToString(separator = " "))
        out.reverse()
        return out

    }

    private fun infixToPostfix(exprs: ArrayList<Expr>) : ArrayList<Expr> {

        val stack = Stack<Expr>()
        val out = arrayListOf<Expr>()

        for (expr in exprs) {
            when (expr) {
                is Operator -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.peek()
                        if (top == Expr.leftParen || (top is Operator && expr.order < top.order)) break
                        out.add(stack.pop())
                    }
                    stack.push(expr)
                }
                Expr.leftParen -> stack.push(expr)
                Expr.rightParen -> {
                    while (stack.isNotEmpty()) {
                        if (stack.peek() == Expr.leftParen) {
                            stack.pop()
                            break
                        }
                        out.add(stack.pop())
                    }
                }
                else -> out.add(expr)
            }
        }

        while (stack.isNotEmpty()) {
            // if (stack.peek() == Expr.leftParen) return null
            out.add(stack.pop())
        }

        return out

    }

    private fun postfixToGlsl(input: ArrayList<Expr>) : Expr? {

        val exprs = ArrayList(input)
        val stack = Stack<Expr>()

        while (exprs.isNotEmpty()) {

            // Log.d("SHAPE", "stack: ${stack.joinToString("  ", transform = { expr -> "$expr(${expr.precision.name[0]})" })}")

            when (val expr = exprs.removeAt(0)) {
                is Operator -> {
                    val args = arrayListOf<Expr>()
                    for (i in 0 until expr.numArgs) {
                        if (stack.isEmpty()) return null
                        args.add(0, stack.pop())
                    }
                    stack.push(expr.of(args))
                }
                else -> stack.push(expr)
            }

        }

        return when (stack.size) {
            0 -> null
            1 -> stack.pop()
            else -> null
        }

    }

}