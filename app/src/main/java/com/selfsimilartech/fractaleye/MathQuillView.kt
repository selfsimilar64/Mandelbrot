package com.selfsimilartech.fractaleye

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.WebView

enum class Keystroke(val str: String) { LEFT("Left"), RIGHT("Right"), BACKSPACE("Backspace") }

class MathQuillView : WebView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

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

    fun getLatex(onValueReceived: (String) -> Unit) {
        evaluateJavascript("javascript:mathField.latex()") {
            Log.e("MQ", "result: $it")
            onValueReceived(it.substring(1..it.length-2))
        }
    }

    fun setLatex(latex: String) {
        evaluateJavascript("javascript:mathField.latex('$latex')", null)
    }

}