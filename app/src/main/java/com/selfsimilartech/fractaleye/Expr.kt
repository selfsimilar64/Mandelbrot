package com.selfsimilartech.fractaleye

import android.util.Log

enum class ExprType { REAL, COMPLEX, NONE }
enum class Precision { SINGLE, DUAL, NONE }
enum class ZoomLevel { SHALLOW, DEEP, NONE }

open class Expr(

        val latex           : String,
        val type            : ExprType   = ExprType.NONE,
        precision           : Precision  = Precision.SINGLE,
        var zoomLevel       : ZoomLevel  = ZoomLevel.DEEP,
        str                 : String     = "",
        val imgId           : Int        = -1,
        val isCmd           : Boolean    = false,
        val insertParens    : Boolean    = false,
        val leftAfter       : Boolean    = false,
        val rightAfter      : Boolean    = false,
        val isConstant      : Boolean    = false

) {

    constructor(other: Expr) : this(
        other.latex,
        other.type,
        other.precision,
        other.zoomLevel,
        other.str,
        other.imgId,
        other.isCmd,
        other.insertParens,
        other.leftAfter,
        other.rightAfter,
        other.isConstant
    )


    companion object {

        val leftParen = Expr("(")
        val rightParen = Expr(")")
        val parens = Expr("", imgId = R.drawable.key_parens, insertParens = true, leftAfter = true)

        val zero    = Expr("0", imgId = R.drawable.key_0)
        val one     = Expr("1", imgId = R.drawable.key_1)
        val two     = Expr("2", imgId = R.drawable.key_2)
        val three   = Expr("3", imgId = R.drawable.key_3)
        val four    = Expr("4", imgId = R.drawable.key_4)
        val five    = Expr("5", imgId = R.drawable.key_5)
        val six     = Expr("6", imgId = R.drawable.key_6)
        val seven   = Expr("7", imgId = R.drawable.key_7)
        val eight   = Expr("8", imgId = R.drawable.key_8)
        val nine    = Expr("9", imgId = R.drawable.key_9)
        val decimal = Expr(".", imgId = R.drawable.key_decimal)

        val z = Expr("z", ExprType.COMPLEX, imgId = R.drawable.key_z)
        val c = Expr("c", ExprType.COMPLEX, imgId = R.drawable.key_c)
        val i = Expr("i", ExprType.COMPLEX, str = "_i", imgId = R.drawable.key_i, isConstant = true)


        val neg   = Operator("neg",        0,  1) { args ->
            Expr("-${args[0]}", args[0].type, args[0].precision)
        }
        val conj  = Operator("overline",   0,  1, imgId = R.drawable.key_conj, isCmd = true) { args ->
            val glsl = if (args[0].isReal) args[0].str else "conj(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val mod   = Operator("||",         0,  1, imgId = R.drawable.key_modulus, str = "modulus", leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "abs(${args[0]})" else "absd(${args[0]})"
            }
            else "cmod(${args[0]})"
            Expr(glsl, ExprType.REAL, args[0].precision)
        }
        val abs   = Operator("abs",        0,  1, imgId = R.drawable.key_abs, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "abs(${args[0]})"
                else                  "absd(${args[0]})"
            }
            else "cabs(${args[0]})"
            val newType = args[0].type
            val newPrecision = args[0].precision
            Expr(glsl, newType, newPrecision)
        }
        val rabs  = Operator("rabs",       0,  1, imgId = R.drawable.key_rabs, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "abs(${args[0]})"
                else                  "absd(${args[0]})"
            }
            else "cabsr(${args[0]})"
            val newType = args[0].type
            val newPrecision = args[0].precision
            Expr(glsl, newType, newPrecision)
        }
        val iabs  = Operator("iabs",       0,  1, imgId = R.drawable.key_iabs, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) args[0].str
            else                           "cabsi(${args[0]})"
            val newType = args[0].type
            val newPrecision = args[0].precision
            Expr(glsl, newType, newPrecision)
        }
        val sign  = Operator("sign",       0,  1, imgId = R.drawable.key_sign, isCmd = true, insertParens = true, leftAfter = true) { args ->
            if (args[0].isReal) {
                if (args[0].isSingle) Expr("sign(${args[0]})",   ExprType.REAL, Precision.SINGLE)
                else                  Expr("sign(${args[0]}.x)", ExprType.REAL, Precision.SINGLE)
            }
            else Expr("csign(${args[0]})", ExprType.COMPLEX, args[0].precision)
        }
        val arg   = Operator("arg",        0,  1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arg, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) "arg(${args[0]})" else "carg(${args[0]})"
            Expr(glsl, ExprType.REAL, args[0].precision)
        }
        val sqr   = Operator("^2",         1,  1, imgId = R.drawable.key_sqr, rightAfter = true) { args ->
            val glsl = if (args[0].isReal) "sqr(${args[0]})" else "csqr(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val cube  = Operator("^3",         1,  1, imgId = R.drawable.key_cube, rightAfter = true) { args ->
            val glsl = if (args[0].isReal) "cube(${args[0]})" else "ccube(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val quad  = Operator("^4",         1,  1, imgId = R.drawable.key_quad, rightAfter = true) { args ->
            val glsl = if (args[0].isReal) "quad(${args[0]})" else "cquad(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val quint = Operator("^5",         1,  1, rightAfter = true) { args ->
            val glsl = if (args[0].isReal) "quint(${args[0]})" else "cquint(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val sext  = Operator("^6",         1,  1, rightAfter = true) { args ->
            val glsl = if (args[0].isReal) "sext(${args[0]})" else "csext(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val pow   = Operator("^",          1,  2, imgId = R.drawable.key_pow) { args ->
            when {
                args[1].str == "1.0" -> args[0]
                args[1].str == "2.0" -> sqr.of(listOf(args[0]))
                args[1].str == "3.0" -> cube.of(listOf(args[0]))
                args[1].str == "4.0" -> quad.of(listOf(args[0]))
                args[1].str == "5.0" -> quint.of(listOf(args[0]))
                args[1].str == "6.0" -> sext.of(listOf(args[0]))
                args[1].str == "-1.0" -> inv.of(listOf(args[0]))
                args[1].str == "-2.0" -> inv.of(listOf(sqr.of(listOf(args[0]))))
                args[1].str == "-3.0" -> inv.of(listOf(cube.of(listOf(args[0]))))
                args[1].str == "-4.0" -> inv.of(listOf(quad.of(listOf(args[0]))))
                args[1].str == "-5.0" -> inv.of(listOf(quint.of(listOf(args[0]))))
                args[1].str == "-6.0" -> inv.of(listOf(sext.of(listOf(args[0]))))
                args[0].isComplex && args[1].str.toFloatOrNull()?.run { compareTo(1f) > 0 && rem(1f) == 0f } ?: false -> {
                    val p = args[1].str.toFloat().toInt()
                    Expr("cpow(${args[0]}, $p)", ExprType.COMPLEX, args[0].precision, if (p > 10) ZoomLevel.SHALLOW else ZoomLevel.DEEP)
                }
                args[0].isReal && args[1].isReal -> {
                    if (args[0].isSingle && args[1].isSingle) Expr("pow(${args[0]}, ${args[1]})", ExprType.REAL, Precision.SINGLE)
                    else                                      Expr("powd(${args[0]}, ${args[1]})", ExprType.REAL, Precision.DUAL)
                }
                else -> Expr("cpow(${args[0]}, ${args[1]})", ExprType.COMPLEX, args[0].precision, ZoomLevel.SHALLOW)
            }
        }
        val inv   = Operator("^-1",        1,  1, imgId = R.drawable.key_inverse, rightAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "(1.0/${args[0]})" else "inv(${args[0]})"
            }
            else "cinv(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val sqrt  = Operator("sqrt",       1,  1, imgId = R.drawable.key_sqrt, isCmd = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "sqrt(${args[0]})" else "sqrtd(${args[0]})"
            }
            else "csqrt(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val mult  = Operator("cdot",       2,  2, imgId = R.drawable.key_times, str = "*", isCmd = true) { args ->
            // Log.e("EXPR", "MULT -- arg1: ${args[0]}(${args[0].precision.name[0]}), arg2: ${args[1]}(${args[1].precision.name[0]})")
            if (args[0] == args[1]) sqr.of(listOf(args[0]))

            val glsl = when {
                args[0].isReal && args[1].isReal -> {
                    if (args[0].isSingle && args[1].isSingle) "${args[0]}*${args[1]}"
                    else                                      "mult(${args[0]}, ${args[1]})"
                }
                args[0].isComplex && args[1].isComplex -> "cmult(${args[0]}, ${args[1]})"
                else -> {
                    if      ( args[0].isSingle && args[1].isSingle ) "${args[0]}*${args[1]}"
                    else if ( args[0].isReal   && args[0].isDual   ) "cmult(complex(${args[0]}), ${args[1]})"
                    else if ( args[1].isReal   && args[1].isDual   ) "cmult(${args[0]}, complex(${args[1]}))"
                    else                                             "cmult(${args[0]}, ${args[1]})"
                }
            }
            val newType       = if ( args[0].isComplex || args[1].isComplex ) ExprType.COMPLEX else ExprType.REAL
            val newPrecision  = if ( args[0].isDual    || args[1].isDual    ) Precision.DUAL   else Precision.SINGLE
            Expr(glsl, newType, newPrecision)

        }
        val div   = Operator("/",          2,  2, imgId = R.drawable.key_div)  { args ->

            if (args[0] == args[1]) Expr("1.0", ExprType.REAL, Precision.SINGLE)

            val glsl = when {
                args[0].isReal && args[1].isReal -> {
                    if (args[0].isSingle && args[1].isSingle) "${args[0]}/${args[1]}"
                    else                                      "div(${args[0]}, ${args[1]})"
                }
                args[0].isComplex && args[1].isComplex -> "cdiv(${args[0]}, ${args[1]})"
                else -> {
                    if ( args[0].isSingle && args[1].isSingle && args[0].isComplex ) "${args[0]}/${args[1]}"
                    else if ( args[0].isReal && args[0].isDual ) "cdiv(complex(${args[0]}), ${args[1]})"
                    else if ( args[1].isReal && args[1].isDual ) "cdiv(${args[0]}, complex(${args[1]}))"
                    else                                         "cdiv(${args[0]}, ${args[1]})"
                }
            }
            val newType       = if ( args[0].isComplex || args[1].isComplex ) ExprType.COMPLEX else ExprType.REAL
            val newPrecision  = if ( args[0].isDual    || args[1].isDual    ) Precision.DUAL   else Precision.SINGLE
            Expr(glsl, newType, newPrecision)

        }
        val add   = Operator("+",          3,  2, imgId = R.drawable.key_plus) { args ->

            // Log.e("EXPR", "ADD -- arg1: ${args[0]}(${args[0].precision.name[0]}), arg2: ${args[1]}(${args[1].precision.name[0]})")
            val glsl = when {
                args[0].isReal && args[1].isReal -> {
                    if (args[0].isSingle && args[1].isSingle) "(${args[0]} + ${args[1]})"
                    else                                      "add(${args[0]}, ${args[1]})"
                }
                args[0].isComplex && args[1].isComplex -> {
                    if (args[0].isDual || args[1].isDual) "cadd(${args[0]}, ${args[1]})"
                    else                                  "(${args[0]} + ${args[1]})"
                }
                else -> {
                    if ( args[0].isSingle && args[1].isSingle ) {
                        if (args[0].isReal) "(complex(${args[0]}) + ${args[1]})"
                        else                "(${args[0]} + complex(${args[1]}))"
                    }
                    else if ( args[0].isReal && args[0].isDual ) "cadd(complex(${args[0]}), ${args[1]})"
                    else if ( args[1].isReal && args[1].isDual ) "cadd(${args[0]}, complex(${args[1]}))"
                    else                                         "cadd(${args[0]}, ${args[1]})"
                }
            }
            val newType       = if ( args[0].isComplex || args[1].isComplex ) ExprType.COMPLEX else ExprType.REAL
            val newPrecision  = if ( args[0].isDual    || args[1].isDual    ) Precision.DUAL   else Precision.SINGLE
            Expr(glsl, newType, newPrecision)

        }
        val sub   = Operator("-",          3,  2, imgId = R.drawable.key_minus) { args ->

            val glsl = when {
                args[0].isReal && args[1].isReal -> {
                    if (args[0].isSingle && args[1].isSingle) "(${args[0]} - ${args[1]})"
                    else                                      "sub(${args[0]}, ${args[1]})"
                }
                args[0].isComplex && args[1].isComplex -> {
                    if (args[0].isDual || args[1].isDual) "cadd(${args[0]}, -${args[1]})"
                    else                                  "(${args[0]} - ${args[1]})"
                }
                else -> {
                    if ( args[0].isSingle && args[1].isSingle ) {
                        if (args[0].isReal) "(complex(${args[0]}) - ${args[1]})"
                        else                "(${args[0]} - complex(${args[1]}))"
                    }
                    else if ( args[0].isReal && args[0].isDual ) "cadd(complex(${args[0]}), -${args[1]})"
                    else if ( args[1].isReal && args[1].isDual ) "cadd(${args[0]}, -complex(${args[1]}))"
                    else                                         "cadd(${args[0]}, -${args[1]})"
                }
            }
            val newType       = if ( args[0].isComplex || args[1].isComplex ) ExprType.COMPLEX else ExprType.REAL
            val newPrecision  = if ( args[0].isDual    || args[1].isDual    ) Precision.DUAL   else Precision.SINGLE
            Expr(glsl, newType, newPrecision)

        }



        val sin = Operator("sin", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_sin, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "sin(${args[0]})" else "sind(${args[0]})"
            }
            else "csin(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val cos = Operator("cos", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_cos, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "cos(${args[0]})" else "cosd(${args[0]})"
            }
            else "ccos(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val tan = Operator("tan", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_tan, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "tan(${args[0]})" else "tand(${args[0]})"
            }
            else "ctan(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val csc = Operator("csc", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_csc, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "csc(${args[0]})" else "cscd(${args[0]})"
            }
            else "ccsc(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val sec = Operator("sec", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_sec, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "sec(${args[0]})" else "secd(${args[0]})"
            }
            else "csec(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val cot = Operator("cot", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_cot, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "cot(${args[0]})" else "cotd(${args[0]})"
            }
            else "ccot(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }

        val asin = Operator("arcsin", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arcsin, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "asin(${args[0]})" else "asind(${args[0]})"
            }
            else "casin(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val acos = Operator("arccos", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arccos, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "acos(${args[0]})" else "acosd(${args[0]})"
            }
            else "cacos(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val atan = Operator("arctan", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arctan, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "atan(${args[0]})" else "atand(${args[0]})"
            }
            else "catan(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val acsc = Operator("arccsc", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arccsc, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "acsc(${args[0]})" else "acscd(${args[0]})"
            }
            else "cacsc(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val asec = Operator("arcsec", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arcsec, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "asec(${args[0]})" else "asecd(${args[0]})"
            }
            else "casec(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val acot = Operator("arccot", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arccot, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "acot(${args[0]})" else "acotd(${args[0]})"
            }
            else "cacot(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }



        val sinh = Operator("sinh", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_sinh, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "sinh(${args[0]})" else "sinhd(${args[0]})"
            }
            else "csinh(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val cosh = Operator("cosh", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_cosh, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "cosh(${args[0]})" else "coshd(${args[0]})"
            }
            else "ccosh(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val tanh = Operator("tanh", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_tanh, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "tanh(${args[0]})" else "tanhd(${args[0]})"
            }
            else "ctanh(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val csch = Operator("csch", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_csch, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "csch(${args[0]})" else "cschd(${args[0]})"
            }
            else "ccsch(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val sech = Operator("sech", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_sech, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "sech(${args[0]})" else "sechd(${args[0]})"
            }
            else "csech(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val coth = Operator("coth", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_coth, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "coth(${args[0]})" else "cothd(${args[0]})"
            }
            else "ccoth(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }

        val asinh = Operator("arcsinh", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arcsinh, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "asinh(${args[0]})" else "asinhd(${args[0]})"
            }
            else "casinh(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val acosh = Operator("arccosh", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arccosh, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "acosh(${args[0]})" else "acoshd(${args[0]})"
            }
            else "cacosh(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val atanh = Operator("arctanh", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arctanh, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "atanh(${args[0]})" else "atanhd(${args[0]})"
            }
            else "catanh(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val acsch = Operator("arccsch", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arccsch, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "acsch(${args[0]})" else "acschd(${args[0]})"
            }
            else "cacsch(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val asech = Operator("arcsech", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arcsech, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "asech(${args[0]})" else "asechd(${args[0]})"
            }
            else "casech(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }
        val acoth = Operator("arccoth", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_arccoth, isCmd = true, insertParens = true, leftAfter = true) { args ->
            val glsl = if (args[0].isReal) {
                if (args[0].isSingle) "acoth(${args[0]})" else "acothd(${args[0]})"
            }
            else "cacoth(${args[0]})"
            Expr(glsl, args[0].type, args[0].precision)
        }



        val ln    = Operator("ln",     0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_ln, insertParens = true, leftAfter = true) { args ->
            if (args[0].isReal) {
                if (args[0].isSingle)   Expr("log(${args[0]})",  ExprType.REAL, Precision.SINGLE)
                else                    Expr("logd(${args[0]})", ExprType.REAL, Precision.DUAL)
            }
            else Expr("clog(${args[0]})", ExprType.COMPLEX, args[0].precision)
        }
        val log2  = Operator("log_2",  0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_log2, insertParens = true, leftAfter = true) { args ->
            if (args[0].isReal) {
                if (args[0].isSingle)   Expr("log2(${args[0]})",  ExprType.REAL, Precision.SINGLE)
                else                    Expr("log2d(${args[0]})", ExprType.REAL, Precision.DUAL)
            }
            else Expr("clog2(${args[0]})", ExprType.COMPLEX, args[0].precision)
        }
        val log10 = Operator("log_10", 0, 1, zoomLevel = ZoomLevel.SHALLOW, imgId = R.drawable.key_log10, str = "log_(10)", insertParens = true, leftAfter = true) { args ->
            if (args[0].isReal) {
                if (args[0].isSingle)   Expr("log10(${args[0]})",  ExprType.REAL, Precision.SINGLE)
                else                    Expr("log10d(${args[0]})", ExprType.REAL, Precision.DUAL)
            }
            else Expr("clog10(${args[0]})", ExprType.COMPLEX, args[0].precision)
        }
//        val log = Operator("log_()", 0, 1, insertParens = true, leftAfter = true) { args, _ ->
//            if (args[0].isReal) Expr("log2(${args[0]})", ExprType.REAL)
//            else Expr ("clog2(${args[0]})", ExprType.COMPLEX)
//        }

        val exp = Operator("e^", 0, 1) { args ->
            if (args[0].isReal) Expr("exp(${args[0]})", ExprType.REAL)
            else Expr("cexp(${args[0]})", ExprType.COMPLEX)
        }
        val exp2 = Operator("2^", 0, 1) {args ->
            if (args[0].isReal) Expr("exp2(${args[0]})", ExprType.REAL)
            else Expr("cexp2(${args[0]})", ExprType.COMPLEX)
        }
        val exp10 = Operator("10^", 0, 1) {args ->
            if (args[0].isReal) Expr("exp10(${args[0]})", ExprType.REAL)
            else Expr("cexp10(${args[0]})", ExprType.COMPLEX)
        }


        val variables = listOf(
                z,
                c,
                parens
        )

        val numbers = listOf(
                one,
                two,
                three,
                four,
                five,
                six,
                seven,
                eight,
                nine,
                zero,
                decimal,
                i
        )

        val basic = listOf(
            add,
            sub,
            mult,
            div,
            sqr,
            cube,
            quad,
            inv,
            sqrt,
            pow,
            abs,
            rabs,
            iabs,
            conj,
            mod,
            arg,
            ln,
            log2,
            log10
        )

        val logarithmic = listOf(
            log2, log10
        )

        val trig = listOf(
                sin,
                cos,
                tan,
                csc,
                sec,
                cot,
                asin,
                acos,
                atan,
                acsc,
                asec,
                acot,
                sinh,
                cosh,
                tanh,
                csch,
                sech,
                coth,
                asinh,
                acosh,
                atanh,
                acsch,
                asech,
                acoth
        )

        val all = listOf(
                leftParen,
                rightParen,
                z,
                c,
                neg,
                conj,
                mod,
                abs,
                rabs,
                iabs,
                sign,
                arg,
                pow,
                sqrt,
                mult,
                div,
                add,
                sub,
                sin, cos, tan,
                csc, sec, cot,
                asin, acos, atan,
                acsc, asec, acot,
                sinh, cosh, tanh,
                csch, sech, coth,
                asinh, acosh, atanh,
                acsch, asech, acoth,
                ln, log2, log10,
                exp, exp2, exp10
        )

        val MAX_EXPR_LEN = all.maxOf { it.str.length }

        val map = HashMap<String, Expr>().also { m ->
            all.forEach { expr -> m[expr.str] = expr }
            m["i"] = i
        }

        fun valueOf(str: String) : Expr? {
            return when {
                str.matches(Regex("^\\d+$"))            -> Expr("$str.0", ExprType.REAL, Precision.SINGLE, isConstant = true)  // integer :  1234
                str.matches(Regex("^\\.\\d+$"))         -> Expr("0$str",  ExprType.REAL, Precision.SINGLE, isConstant = true)  // float   :  .1234
                str.matches(Regex("^(\\d*\\.)?\\d+$"))  -> Expr(str,      ExprType.REAL, Precision.SINGLE, isConstant = true)  // float   :  0.1234
                else -> map[str]
            }
        }

    }


    val str = if (str == "") latex else str

    private val isReal = type == ExprType.REAL

    private val isComplex = type == ExprType.COMPLEX

    private var isSingle = precision == Precision.SINGLE

    private var isDual = precision == Precision.DUAL

    var precision = precision
        set(value) {
            field = value
            isSingle = value == Precision.SINGLE
            isDual = value == Precision.DUAL
        }

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

    latex: String,
    val order: Int,
    val numArgs: Int,
    zoomLevel : ZoomLevel = ZoomLevel.DEEP,
    str: String = "",
    imgId: Int = -1,
    isCmd: Boolean = false,
    insertParens: Boolean = false,
    leftAfter: Boolean = false,
    rightAfter: Boolean = false,
    val operate: (args: List<Expr>) -> Expr

) : Expr(latex, ExprType.NONE, Precision.NONE, zoomLevel, str, imgId, isCmd, insertParens, leftAfter, rightAfter) {

    fun of(args: List<Expr>) : Expr {
        val expr = operate(args)
        expr.zoomLevel = if (args.any { it.zoomLevel == ZoomLevel.SHALLOW } || zoomLevel == ZoomLevel.SHALLOW) ZoomLevel.SHALLOW else ZoomLevel.DEEP
        return expr
    }

}
