package com.selfsimilartech.fractaleye

import android.util.Log

enum class ExprType { REAL, COMPLEX, NONE }

open class Expr(
        val katex: String,
        val type: ExprType = ExprType.NONE,
        val str: String = katex,
        val isCmd: Boolean = false,
        val insertParens: Boolean = false,
        val leftAfter: Boolean = false,
        val rightAfter: Boolean = false
) {
    
    companion object {
        
        val leftParen = Expr("(")
        val rightParen = Expr(")")
        val parens = Expr("", insertParens = true, leftAfter = true)

        val zero = Expr("0")
        val one = Expr("1")
        val two = Expr("2")
        val three = Expr("3")
        val four = Expr("4")
        val five = Expr("5")
        val six = Expr("6")
        val seven = Expr("7")
        val eight = Expr("8")
        val nine = Expr("9")
        val decimal = Expr(".")

        val z = Expr("z", ExprType.COMPLEX)
        val c = Expr("c", ExprType.COMPLEX)
        val i = Expr("i", ExprType.COMPLEX)


        val neg   = Operator("neg",        0,  1) { args, df ->
            Expr("-${args[0]}", args[0].type)
        }
        val conj  = Operator("\\overline", 0,  1, isCmd = true) { args, df ->
            Expr("conj(${args[0]})", ExprType.COMPLEX)
        }
        val mod   = Operator("||",         0,  1, "|") { args, df ->
            Expr("cmod(${args[0]})", ExprType.REAL)
        }
        val arg   = Operator("\\arg",      0,  1, insertParens = true, leftAfter = true) { args, df ->
            Expr("carg(${args[0]})", ExprType.REAL)
        }
        val sqr   = Operator("^2",         1,  1, rightAfter = true) { args, _ ->
            if (args[0].isReal) Expr("sqr(${args[0]})", ExprType.REAL)
            else Expr("csqr(${args[0]})", ExprType.COMPLEX)
        }
        val cube  = Operator("^3",         1,  1, rightAfter = true) { args, _ ->
            if (args[0].isReal) Expr("cube(${args[0]})", ExprType.REAL)
            else Expr("ccube(${args[0]})", ExprType.COMPLEX)
        }
        val quad  = Operator("^4",         1,  1, rightAfter = true) { args, _ ->
            if (args[0].isReal) Expr("quad(${args[0]})", ExprType.REAL)
            else Expr("cquad(${args[0]})", ExprType.COMPLEX)
        }
        val quint = Operator("^5",         1,  1, rightAfter = true) { args, _ ->
            if (args[0].isReal) Expr("quint(${args[0]})", ExprType.REAL)
            else Expr("cquint(${args[0]})", ExprType.COMPLEX)
        }
        val pow   = Operator("^",          1,  2)  { args, _ ->
            when {
                args[1].str == "2.0" -> sqr.of(listOf(args[0]), false)
                args[1].str == "3.0" -> cube.of(listOf(args[0]), false)
                args[1].str == "4.0" -> quad.of(listOf(args[0]), false)
                args[1].str == "5.0" -> quint.of(listOf(args[0]), false)
                args[1].str == "-1.0" -> inv.of(listOf(args[0]), false)
                args[0].isReal && args[1].isReal -> Expr("pow(${args[0]}, ${args[1]})", ExprType.REAL)
                else -> Expr("cpow(${args[0]}, ${args[1]})", ExprType.COMPLEX)
            }
        }
        val inv   = Operator("^-1",        1,  1, rightAfter = true) { args, _ ->
            if (args[0].isReal) Expr("(1.0/${args[0]})", ExprType.REAL)
            else Expr("cinv(${args[0]})", ExprType.COMPLEX)
        }
        val sqrt  = Operator("\\sqrt",     1,  1, isCmd = true) { args, _ ->
            if (args[0].isReal) Expr("sqrt(${args[0]})", ExprType.REAL)
            else Expr("csqrt(${args[0]})", ExprType.COMPLEX)
        }
        val mult  = Operator("\\cdot",     2,  2,  "*", isCmd = true) { args, df ->
            when {
                args[0] == args[1] -> sqr.of(listOf(args[0]), df)
                df || (args[0].isComplex && args[1].isComplex) -> Expr("cmult(${args[0]}, ${args[1]})", ExprType.COMPLEX)
                args[0].isReal && args[1].isReal -> Expr("${args[0]}*${args[1]}", ExprType.REAL)
                else -> Expr("${args[0]}*${args[1]}", ExprType.COMPLEX)
            }
        }
        val div   = Operator("\\frac",     2,  2,  "/")  { args, df ->
            when {
                df || args[1].isComplex -> Expr("cdiv(${args[0]}, ${args[1]})", ExprType.COMPLEX)
                args[0].isReal && args[1].isReal -> Expr("(${args[0]}/${args[1]})", ExprType.REAL)
                else -> Expr("${args[0]}/(${args[1]})", ExprType.COMPLEX)
            }
        }
        val add   = Operator("+",          3,  2)  { args, df ->
            Log.e("EXPR", "arg0: ${args[0]}, type: ${args[0].type}")
            Log.e("EXPR", "arg1: ${args[1]}, type: ${args[1].type}")
            when {
                df -> Expr("cadd(${args[0]}, ${args[1]})", ExprType.COMPLEX)
                args[0].isReal && args[1].isReal -> Expr("(${args[0]} + ${args[1]})", ExprType.REAL)
                args[0].isComplex && args[1].isReal -> Expr("(${args[0]} + vec2(${args[1]}, 0.0))", ExprType.COMPLEX)
                args[0].isReal && args[1].isComplex -> Expr("(${args[1]} + vec2(${args[0]}, 0.0))", ExprType.COMPLEX)
                else -> Expr("(${args[0]} + ${args[1]})", ExprType.COMPLEX)
            }
        }
        val sub   = Operator("-",          3,  2)  { args, df ->
            when {
                df -> Expr("cadd(${args[0]}, -${args[1]})", ExprType.COMPLEX)
                args[0].isReal && args[1].isReal -> Expr("(${args[0]} - ${args[1]})", ExprType.REAL)
                args[0].isComplex && args[1].isReal -> Expr("(${args[0]} + vec2(-${args[1]}, 0.0))", ExprType.COMPLEX)
                args[0].isReal && args[1].isComplex -> Expr("(${args[1]} + vec2(-${args[0]}, 0.0))", ExprType.COMPLEX)
                else -> Expr("(${args[0]} - ${args[1]})", ExprType.COMPLEX)
            }
        }

        val sin = Operator("sin", 0, 1, "\\sin", isCmd = true) { args, _ ->
            if (args[0].isReal) Expr("sin(${args[0]})", ExprType.REAL)
            else Expr ("csin(${args[0]})", ExprType.COMPLEX)
        }


        val numbers = listOf(
                zero,
                one,
                two,
                three,
                four,
                five,
                six,
                seven,
                eight,
                nine
        )

        val all = listOf(
                leftParen,
                rightParen,
                z,
                c,
                neg,
                conj,
                mod,
                arg,
                pow,
                sqrt,
                mult,
                div,
                add,
                sub,
                sin
        )

        fun valueOf(str: String) : Expr? {
            all.forEach { if (it.str == str) return it }
            return when {
                str == "i" -> Expr("I", ExprType.COMPLEX)
                str.matches(Regex("-?[1-9]+[0-9]*")) -> Expr("$str.0", ExprType.REAL)  // integer
                str.matches(Regex("-?[0-9]+.?[0-9]+")) -> Expr(str, ExprType.REAL)     // float
                else -> null
            }
        }
        
    }

    private val isReal = type == ExprType.REAL

    private val isComplex = type == ExprType.COMPLEX

    override fun equals(other: Any?): Boolean {
        return other is Expr && str == other.str
    }

    override fun toString(): String {
        return str
    }

    override fun hashCode(): Int {
        return str.hashCode()
    }

}

class Operator(

        str: String,
        val order: Int,
        val numArgs: Int,
        katex: String = str,
        isCmd: Boolean = false,
        insertParens: Boolean = false,
        leftAfter: Boolean = false,
        rightAfter: Boolean = false,
        val of: (args: List<Expr>, df: Boolean) -> Expr

) : Expr(str, ExprType.NONE, katex, isCmd, insertParens, leftAfter, rightAfter)
