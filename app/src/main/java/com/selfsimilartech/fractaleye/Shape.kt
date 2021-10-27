package com.selfsimilartech.fractaleye

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Range
import kotlin.math.sqrt


class Shape(

    var id                      : Int               = -1,
    var hasCustomId             : Boolean           = false,
    val nameId                  : Int               = -1,
    override var name           : String            = "",
    val latexId                 : Int               = -1,
    var latex                   : String            = "",
    val thumbnailId             : Int               = R.drawable.mandelbrot_icon,
    override var thumbnail      : Bitmap?           = null,
    conditional                 : String            = ESCAPE,
    val init                    : String            = "",
    val loop                    : String            = "",
    val final                   : Int               = R.string.empty,
    var delta1                  : String            = "",
    var deltaJulia1             : String            = "",
    var alphaSeed               : Complex           = Complex.ZERO,
    val hasAnalyticDelta        : Boolean           = false,
    compatTextures              : List<Texture>     = Texture.divergent,
    val positions               : PositionList      = PositionList(),
    val params                  : ParamSet          = ParamSet(),
    val randomConfigs           : List<Config>      = listOf(),
    juliaMode                   : Boolean           = false,
    val juliaSeed               : Boolean           = false,
    var maxIter                 : Int               = 256,
    radius                      : Float             = 1e6f,
    val power                   : Float             = 2f,
    val hasDynamicPower         : Boolean           = false,
    val isConvergent            : Boolean           = false,
    override val goldFeature    : Boolean           = false,
    val devFeature              : Boolean           = false,
    val isTranscendental        : Boolean           = false,
    val hasDualFloat            : Boolean           = true,
    var slowDualFloat           : Boolean           = false,
    val hasPerturbation         : Boolean           = false,
    var customLoopSingle        : String            = "",
    var customLoopDual          : String            = "",
    override var isFavorite     : Boolean           = false,
    var iterateNative           : (d: ScriptField_IterateData) -> FloatArray = { _ -> floatArrayOf() }

) : Customizable, Goldable {


    class ParamSetPreset(
        val list: List<RealParam> = listOf(),
        val julia: ComplexParam? = null,
        val seed: ComplexParam? = null
    )

    class ParamSet(
        val list: List<RealParam> = listOf(),
        val julia: ComplexParam = ComplexParam(),
        val seed: ComplexParam = ComplexParam()
    ) {

        val size = list.size
        var active: RealParam = julia

        fun at(index: Int) = list[index]
        fun setFrom(newSet: ParamSet) {

            julia.setFrom(newSet.julia)
            seed.setFrom(newSet.seed)
            list.forEachIndexed { i, p ->
                newSet.list.find { it.nameId == p.nameId }?.let { p.setFrom(it) }
            }

        }

        fun setFrom(newParams: ParamSetPreset) {

            newParams.julia?.let { julia.setFrom(it) }
            newParams.seed?.let { seed.setFrom(it) }
            list.forEachIndexed { i, p ->
                val newParam = newParams.list.getOrNull(i)
                if (newParam != null) p.setFrom(newParam)
            }

        }

        fun reset() {

            list.forEach { it.reset() }
            julia.reset()
            seed.reset()

        }

        fun clone(): ParamSetPreset {
            return ParamSetPreset(
                List(list.size) { i -> list[i].clone() },
                julia = julia.clone(),
                seed = seed.clone()
            )
        }

        fun toConstructorString(): String {
            return "\n\tParamSetPreset(listOf(${list.joinToString { it.toConstructorString() }}))"
        }

    }

    class Config(
        val position    : Position?         = null,
        val params      : ParamSetPreset?   = null,
        val juliaMode   : Boolean?          = null
    ) {
        fun isGoldFeature(shape: Shape) : Boolean {
            params?.list?.forEachIndexed { i, p -> if (shape.params.list[i].goldFeature && !shape.params.list[i].valueEquals(p)) return true }
            if (juliaMode == true && !shape.juliaModeInit && shape != mandelbrot) return true
            return false
        }
    }


    companion object {

        private var nextId = 0
            get() = field++

        private const val ESCAPE = "escape(modsqrz, z)"
        private const val CONVERGE = "converge(eps, z, z1)"

        const val CUSTOM_LOOP = "customshape_loop(z1, c)"


        val emptyFavorite = Shape(name = "Empty Favorite")
        val emptyCustom = Shape(name = "Empty Custom")
        val testshape = Shape(
            nameId = R.string.shape,
            loop = "testshape(z1, c)",
            conditional = CONVERGE,
            isConvergent = true,
            goldFeature = true
        )

        fun createNewCustom(res: Resources): Shape {
            return Shape(
                name = "%s %s %d".format(
                    res.getString(R.string.header_custom),
                    res.getString(R.string.shape),
                    nextCustomShapeNum
                ),
                latex = "z^2 + c",
                loop = CUSTOM_LOOP,
                customLoopSingle = "csqr(z) + c",
                customLoopDual = "cadd(csqr(z), c)",
                positions = PositionList(Position(zoom = 5e0)),
                hasDualFloat = true
            )
        }

        val mandelbrot = Shape(
            id = nextId,
            nameId = R.string.mandelbrot,
            latexId = R.string.mandelbrot_latex,
            thumbnailId = R.drawable.mandelbrot_icon,
            loop = "mandelbrot(z1, c)",
            delta1 = "mandelbrot_delta1(alpha, z1)",
            deltaJulia1 = "mandelbrot_julia_delta1(alpha, z1)",
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(main = Position(x = -0.75, zoom = 3.5)),
            randomConfigs = listOf(
                Config(Position(x = 0.3229090457868484,    y = 0.44956410983655004,  zoom = 2.48e-11)),
                Config(Position(x = 0.0650067337017734,    y = -0.6358224558588296,  zoom = 5.12e-11)),
                Config(Position(x = -0.22868757498633432,  y = -0.6984316911776989,  zoom = 1.56e-11)),
                Config(Position(x = 0.32670837276621506,   y = -0.5807359395617241,  zoom = 9.61e-11)),
                // Config(Position(x = -0.019073630016756084, y = 0.006914950394179597, zoom = 4.89e-10)),
                Config(Position(x = -0.23228404049167914,  y = -0.6940701025417396,  zoom = 2.65e-12)),
                Config(Position(x = -1.777299731770411,    y = 5.150549478325214E-4, zoom = 6.42e-12)),
                Config(Position(x = -0.006900008158934119, y = -0.8070917121437601,  zoom = 9.68e-12)),
                Config(Position(x = -0.37409461180508924,  y = -0.6599464408230499,  zoom = 7.11e-11)),
                Config(Position(x = -0.05053703472132038,  y = -0.6518886218998218,  zoom = 2.42e-12)),
                Config(Position(x = 0.33313599575803904,   y = -0.04971724553148185, zoom = 4.71e-10)),
                Config(Position(x = -0.7793725285028502,   y = -0.1352493340114603,  zoom = 3.19e-11)),
                Config(Position(x = -1.3228177325253214,   y = 0.08328798108193408,  zoom = 1.98e-9)),
                Config(Position(x = -0.7877246947962989,   y = 0.1557008043679318,   zoom = 1.18e-11)),
                Config(Position(x = -0.9143368808649641,   y = -0.23675929100401913, zoom = 2.71e-12)),
                Config(Position(x = -1.4753299325610634,   y = 0.004123950207165176, zoom = 2.66e-9)),
                Config(Position(x = -0.6903196519428226,   y = 0.29077664182667395,  zoom = 5.21e-11)),
                Config(Position(x = -0.6895722661144748,   y = 0.2955682764155646,   zoom = 2.89e-10)),
                Config(Position(x = -1.7676398388824568,   y = 0.005438174441339262, zoom = 8.47e-11)),
                Config(Position(x = -1.674410903729111,    y = -3.06634353319223E-4, zoom = 5.88e-12)),
                Config(Position(x = 0.3689763583406811,    y = 0.09359715261161919,  zoom = 1.11e-11)),
                Config(Position(x = -0.21923568026968576,  y = -0.6964679840598474,  zoom = 1.38e-9)),
                Config(Position(x = -1.1202783562484184,   y = -0.01068235251833857, zoom = 4.28e-12)),
                Config(Position(x = -1.4742797671188836,   y = -3.8709905957775E-13, zoom = 1.57e-9)),
                Config(Position(x = -1.7687911339372593,   y = 0.004424679892528449, zoom = 2.83e-10)),
                Config(Position(x = -0.5291375239727468,   y = 0.5931785250849431,   zoom = 6.77e-11)),
            ),
            maxIter = 512,
            hasPerturbation = true
        )
        val mandelbrotCubic = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_cubic,
            latexId = R.string.mandelbrot_cubic_latex,
            thumbnailId = R.drawable.mandelbrotcubic_icon,
            loop = "mandelbrot_cubic(z1, c)",
            delta1 = mandelbrot.delta1,
            deltaJulia1 = mandelbrot.deltaJulia1,
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(main = Position(zoom = 3.5)),
            randomConfigs = listOf(
                Config(Position(x = -0.3623718400568671, y = -0.7013147644724544, zoom = 1.301599e-10)),
                Config(Position(x = -0.3623721870976059, y = -0.7013149241898085, zoom = 9.728895e-12)),
                Config(Position(x = -0.5807851581128071, y = 0.7196315078312107, zoom = 1.219373e-11)),
                Config(Position(x = 0.5533540380010176, y = 0.6100801775466154, zoom = 3.330337e-11)),
                Config(Position(x = -0.4054438679901082, y = -0.004244733720585924, zoom = 3.441673e-11)),
                Config(Position(x = 0.450559655419769, y = 0.02588668793654735, zoom = 2.502040e-11)),
                Config(Position(x = -0.26912602309361144, y = 1.2662350974005974, zoom = 3.819688e-12)),
                Config(Position(x = -0.02957315647235126, y = -1.1231254768357586, zoom = 8.467150e-12)),
                Config(Position(x = -0.45456949409824593, y = -0.03683668288299407, zoom = 2.325230e-12)),
                Config(Position(x = 0.627330220806341, y = 0.319005706477124, zoom = 2.157418e-12))
            ),
            power = 3f,
            maxIter = 512,
            goldFeature = true
        )
        val mandelbrotQuartic = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_quartic,
            latexId = R.string.mandelbrot_quartic_latex,
            thumbnailId = R.drawable.mandelbrotquartic_icon,
            loop = "mandelbrot_quartic(z1, c)",
            delta1 = mandelbrot.delta1,
            deltaJulia1 = mandelbrot.deltaJulia1,
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(main = Position(x = -0.175, zoom = 3.5)),
            randomConfigs = listOf(
                Config(Position(x = -1.1202783562484184, y = -0.010682352518338574, zoom = 4.280648e-12)),
                Config(Position(x = -0.710683297225126, y = -0.4909333025435377, zoom = 4.206267e-11)),
                Config(Position(x = 0.5489286638086789, y = -0.0416709252246268, zoom = 6.182704e-12)),
                Config(Position(x = -1.203500947760224, y = 6.319318795700637E-5, zoom = 2.674834e-12)),
                Config(Position(x = -0.12166862384001641, y = -0.8281050275260702, zoom = 4.954608e-12)),
                Config(Position(x = -0.7042473562036579, y = -0.3131896821770736, zoom = 1.704733e-11)),
                Config(Position(x = 0.6179661010149741, y = 0.4646664280490811, zoom = 7.463394e-12)),
                Config(Position(x = 0.589115538292486, y = 0.06409886360315657, zoom = 3.415799e-12)),
                Config(Position(x = -1.1492355052852323, y = -0.22698264031347068, zoom = 2.127199e-11)),
                Config(Position(x = -0.09366512020301405, y = -0.7637558112360573, zoom = 1.175214e-11))
            ),
            power = 4f,
            maxIter = 512,
            goldFeature = true
        )
        val mandelbrotQuintic = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_quintic,
            latexId = R.string.mandelbrot_quintic_latex,
            thumbnailId = R.drawable.mandelbrotquintic_icon,
            loop = "mandelbrot_quintic(z1, c)",
            delta1 = mandelbrot.delta1,
            deltaJulia1 = mandelbrot.deltaJulia1,
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(main = Position(zoom = 3.5)),
            randomConfigs = listOf(
                Config(Position(x = -0.004226475185236266,  y = -0.5569756486518741,    zoom = 9.999999e-12)),
                Config(Position(x = -0.2909621321796368,    y = -0.7794268216344981,    zoom = 1.203149e-11)),
                Config(Position(x = -0.5835603719035269,    y =  0.8751228627316919,    zoom = 2.451157e-11)),
                Config(Position(x =  0.7896144401499247,    y = -0.23556152576168804,   zoom = 9.886303e-13)),
                Config(Position(x =  0.3277681201567144,    y = -0.7865963631027408,    zoom = 5.042429e-12)),
                Config(Position(x = -0.8057489644805943,    y = -0.28267020290231243,   zoom = 6.026693e-12)),
                Config(Position(x = -0.8877891628866241,    y =  0.7604521544210621,    zoom = 3.237718e-11)),
                Config(Position(x = -0.29002718595415455,   y = -0.8250894127119175,    zoom = 6.846398e-12)),
                Config(Position(x =  0.6644275639088228,    y = -0.43653179300250017,   zoom = 2.296070e-11)),
                Config(Position(x =  0.6113432793184647,    y = -0.5337772830467558,    zoom = 1.023339e-11))
            ),
            power = 5f,
            maxIter = 512,
            goldFeature = true
        )
        val mandelbrotSextic = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_sextic,
            latexId = R.string.mandelbrot_sextic_latex,
            thumbnailId = R.drawable.mandelbrotsextic_icon,
            loop = "mandelbrot_sextic(z1, c)",
            delta1 = mandelbrot.delta1,
            deltaJulia1 = mandelbrot.deltaJulia1,
            hasAnalyticDelta = true,
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(main = Position(zoom = 3.5)),
            randomConfigs = listOf(
                Config(Position(x = -0.5173643233402634, y = -0.4514428308629387, zoom = 2.297199e-11)),
                Config(Position(x = 0.7170050067433724, y = -0.7480764548216035, zoom = 2.857234e-12)),
                Config(Position(x = -1.0612067329303374, y = -0.07097398354340238, zoom = 2.838096e-12)),
                Config(Position(x = 0.18152475087532377, y = -0.5617785771825108, zoom = 2.560007e-12)),
                Config(Position(x = -0.23260386138797803, y = -1.0760892108917073, zoom = 4.437027e-12)),
                Config(Position(x = -1.1413316178723039, y = 0.005663361380034798, zoom = 5.592060e-12)),
                Config(Position(x = -0.8090305732122246, y = 0.10925408238667988, zoom = 1.748498e-11)),
                Config(Position(x = 0.797538098424774, y = -0.1762014002102236, zoom = 1.143388e-10)),
                Config(Position(x = 0.6230494724982564, y = -0.5315533518575541, zoom = 3.336067e-11)),
                Config(Position(x = -0.7958595031311616, y = -0.30923062834753196, zoom = 4.403475e-12))
            ),
            power = 6f,
            maxIter = 512,
            goldFeature = true
        )
        val mandelbrotPow = Shape(
            id = nextId,
            nameId = R.string.mandelbrot_anypow,
            thumbnailId = R.drawable.mandelbrotanypow_icon,
            loop = "mandelbrot_power(z1, c)",
            compatTextures = Texture.mandelbrot.minus(Texture.triangleIneqAvgFloat),
            positions = PositionList(Position(x = -0.55, y = 0.5, zoom = 5.0)),
            params = ParamSet(listOf(ComplexParam(R.string.power, u = 16.0, v = 4.0))),
            hasDynamicPower = true,
            slowDualFloat = true,
            goldFeature = true
        )
        val clover = Shape(
            id = nextId,
            nameId = R.string.clover,
            latexId = R.string.clover_latex,
            thumbnailId = R.drawable.clover_icon,
            loop = "clover(z1, c)",
            compatTextures = Texture.divergent.minus(Texture.fieldLines),
            positions = PositionList(Position(zoom = 2.0, rotation = 45.0.inRadians())),
            params = ParamSet(
                listOf(RealParam(R.string.power, 2.0, Range(2.0, 6.0), discrete = true)),
                seed = ComplexParam(u = 1.0)
            ),
            randomConfigs = listOf(
                Config(Position(x = -0.31933804043540587,   y = -0.49530850362822876,   zoom = 1.200353e-10)),
                Config(Position(x =  0.007207687119137132,  y =  0.589361725239222,     zoom = 2.243560e-11)),
                Config(Position(x = -0.3918606992290505,    y = -0.5134288391783682,    zoom = 1.240732e-11)),
                Config(Position(x =  0.20791113354006216,   y =  0.460040505846335,     zoom = 1.122674e-10)),
                Config(Position(x =  0.1255543876970117,    y = -0.36404566366265184,   zoom = 5.010300e-11)),
                Config(Position(x =  0.4636938917559101,    y = -0.4665767909926029,    zoom = 1.073204e-11)),
                Config(Position(x =  0.32261916779913746,   y = -0.058779002664543174,  zoom = 7.563537e-11)),
                Config(Position(x = -6.990839315860789E-4,  y = -0.0029691034535613103, zoom = 6.878362e-12)),
                Config(Position(x =  0.5560806373146088,    y = -0.1509273275100025,    zoom = 3.867315e-12)),
                Config(Position(x =  0.15161668902359127,   y = -0.2625967032054415,    zoom = 5.903969e-12),   ParamSetPreset(listOf(RealParam(u = 3.0)))),
                Config(Position(x = -0.5444641602894895,    y = -0.03416478201660765,   zoom = 5.225113e-12),   ParamSetPreset(listOf(RealParam(u = 3.0)))),
                Config(Position(x =  0.3339179011875168,    y = -0.1928507505572662,    zoom = 4.146764e-12),   ParamSetPreset(listOf(RealParam(u = 3.0)))),
                Config(Position(x = -0.25439295549261903,   y =  1.1710315351436239E-4, zoom = 1.152527e-12),   ParamSetPreset(listOf(RealParam(u = 3.0)))),
                Config(Position(x = -0.41736632224624454,   y = -0.024472860184392792,  zoom = 3.299817e-12),   ParamSetPreset(listOf(RealParam(u = 3.0)))),
                Config(Position(x = -0.10341979334881096,   y = -0.40724334013084373,   zoom = 1.884648e-10),   ParamSetPreset(listOf(RealParam(u = 4.0)))),
                Config(Position(x = -0.371743692983354,     y =  0.36604122407257883,   zoom = 1.385589e-11),   ParamSetPreset(listOf(RealParam(u = 4.0)))),
                Config(Position(x =  0.2967985433167444,    y = -0.3010695314852729,    zoom = 3.759974e-12),   ParamSetPreset(listOf(RealParam(u = 4.0)))),
                Config(Position(x =  0.40834717201170256,   y =  2.3608513195509193E-4, zoom = 2.979418e-11),   ParamSetPreset(listOf(RealParam(u = 4.0)))),
                Config(Position(x = -0.004344223899713354,  y = -0.5870214786703586,    zoom = 6.808881e-12),   ParamSetPreset(listOf(RealParam(u = 5.0)))),
                Config(Position(x = -0.36673898327228127,   y = -0.46631234509475317,   zoom = 7.085350e-12),   ParamSetPreset(listOf(RealParam(u = 5.0)))),
                Config(Position(x = -0.19151773609792244,   y = -0.39270665950437544,   zoom = 4.635963e-12),   ParamSetPreset(listOf(RealParam(u = 7.0)))),

            )
        )
        val mandelbox = Shape(
            id = nextId,
            nameId = R.string.mandelbox,
            thumbnailId = R.drawable.mandelbox_icon,
            loop = "mandelbox(z1, c)",
            positions = PositionList(Position(zoom = 6.5)),
            params = ParamSet(
                listOf(
                    ComplexParam(u = -2.66421354),
                    RealParam(R.string.scale, u = 1.0, uRange = Range(0.0, 5.0)),
                    RealParam(R.string.power, u = 1.0, uRange = Range(1.0, 6.0), discrete = true)
                )
            ),
            radius = 5f,
            randomConfigs = listOf(
                Config(Position(x = -0.33368561433746113, y = 0.169207321179211, zoom = 8.801870e-12)),
                Config(Position(x = -1.8704399407121168, y = 0.023991760638451754, zoom = 7.305231e-12)),
                Config(Position(x = -2.5935127718754394E-8, y = -0.6323158576024894, zoom = 5.420517e-12)),
                Config(
                    Position(x = -2.250116641061081, y = -1.4811927091651675, zoom = 4.227127e-11),
                    ParamSetPreset(listOf(ComplexParam(5.28828876114008, 2.7496482620263096), RealParam(0.5221432579888244), RealParam(2.0)))
                ),
                Config(
                    Position(x = -3.1885698857039517, y = -3.164467655664685, zoom = 1.649809e-08),
                    ParamSetPreset(listOf(ComplexParam(0.0, 2.44555756403729), RealParam(1.0409924643380302), RealParam(3.0)))
                ),
                Config(
                    Position(x = 2.3150593649406366, y = 1.7719250074239616, zoom = 7.287470e-12),
                    ParamSetPreset(listOf(ComplexParam(-2.544941202875759, 1.923812325863271), RealParam(1.3473268236432745), RealParam(1.01)))
                ),
                Config(
                    Position(x = 1.651904710601584, y = 1.7683706839794593, zoom = 3.982938e-12),
                    ParamSetPreset(listOf(ComplexParam(-2.470534467127885, 2.5419605920763644), RealParam(1.1265164027138352), RealParam(1.01)))
                ),
                Config(
                    Position(x = 1.733559272098215, y = -1.951820580838924, zoom = 6.629490e-10),
                    ParamSetPreset(listOf(ComplexParam(-2.6743775735270114, 0.0), RealParam(1.5995156121632415), RealParam(2.0)))
                ),
                Config(
                    Position(x = 2.4595135660039413, y = 2.9977281008817966, zoom = 3.853694e-10),
                    ParamSetPreset(listOf(ComplexParam(-1.6669078678001072, 0.6417665309769117), RealParam(1.231460056607686), RealParam(2.0)))
                ),
                Config(
                    Position(x = -2.3520638786394796, y = -2.2450915610732767, zoom = 6.392506e-09),
                    ParamSetPreset(listOf(ComplexParam(-1.6669078678001072, 0.6417665309769117), RealParam(1.231460056607686), RealParam(2.0)))
                ),
                Config(
                    Position(x = 0.07936350342603574, y = -1.8414852632389167, zoom = 5.621737e-12),
                    ParamSetPreset(listOf(ComplexParam(2.7373813028119054, 0.6758056432891872), RealParam(0.9259045010521297), RealParam(0.0)))
                )
            )
        )
        val kali = Shape(
            id = nextId,
            nameId = R.string.kali,
            thumbnailId = R.drawable.kali_icon,
            loop = "kali(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(zoom = 3.0)),
            params = ParamSet(julia = ComplexParam(u = -0.33170626, v = -0.18423799)),
            radius = 4e0f
        )
        val burningShip = Shape(
            id = nextId,
            nameId = R.string.burning_ship,
            latexId = R.string.burningship_latex,
            thumbnailId = R.drawable.burningship_icon,
            loop = "burning_ship(z1, c)",
            positions = PositionList(Position(-0.4, -0.6, 4.0, Math.PI)),
            randomConfigs = listOf(
                Config(juliaMode = true, params = ParamSetPreset(julia = ComplexParam(u = -1.5731155523848364,  v = -0.026132025751311724))),
                Config(juliaMode = true, params = ParamSetPreset(julia = ComplexParam(u = -0.16795426991357956, v = -1.1109184781049337))),
                Config(juliaMode = true, params = ParamSetPreset(julia = ComplexParam(u = -1.7897574908251732,  v = -0.002678303584761855))),
                Config(juliaMode = true, params = ParamSetPreset(julia = ComplexParam(u =  0.9301460418835946,  v = -1.6110601734211332))),
                Config(juliaMode = true, params = ParamSetPreset(julia = ComplexParam(u = -1.3240783187798868,  v = -0.4268772342040869))),
                Config(juliaMode = true, params = ParamSetPreset(julia = ComplexParam(u =  0.9802220254089256,  v = -1.2147833859402297))),
                Config(juliaMode = true, params = ParamSetPreset(julia = ComplexParam(u = -1.6695749567421272,  v = -1.3027899283736662E-5))),
            )
        )
        val burningShipPow = Shape(
            id = nextId,
            nameId = R.string.burning_ship_anypow,
            thumbnailId = R.drawable.burningshipanypow_icon,
            loop = "burning_ship_power(z1, c)",
            params = ParamSet(listOf(ComplexParam(R.string.power, 6.0, 1.0))),
            positions = PositionList(
                Position(
                    x = 0.15,
                    y = -0.15,
                    zoom = 4.0,
                    rotation = 0.5 * Math.PI
                )
            ),
            devFeature = true
        )
        val magnet1 = Shape(
            id = nextId,
            nameId = R.string.magnet1,
            // latexId = R.string.magnet1_latex,
            thumbnailId = R.drawable.magnet_icon,
            loop = "magnet1(z1, c)",
            positions = PositionList(
                Position(
                    x = 1.25,
                    zoom = 6.0,
                    rotation = (-90.0).inRadians()
                )
            ),
            params = ParamSet(listOf(ComplexParam(u = 1.0, v = 2.0))),
            goldFeature = true
        )
        val magnet2 = Shape(
            id = nextId,
            nameId = R.string.magnet2,
            thumbnailId = R.drawable.magnet2_icon,
            loop = "magnet2(z1, c)",
            positions = PositionList(
                Position(
                    x = 1.25,
                    zoom = 4.0,
                    rotation = (-90.0).inRadians()
                )
            ),
            params = ParamSet(listOf(ComplexParam(u = 1.0, v = 2.0))),
            goldFeature = true
        )
        val sine = Shape(
            id = nextId,
            nameId = R.string.sine,
            latexId = R.string.sine1_latex,
            thumbnailId = R.drawable.sine1_icon,
            loop = "sine(z1, c)",
            positions = PositionList(Position(zoom = 6.0)),
            radius = 3e2f,
            slowDualFloat = true
        )
        val cosine = Shape(
            id = nextId,
            nameId = R.string.cosine,
            latexId = R.string.cosine_latex,
            thumbnailId = R.drawable.cosine_icon,
            loop = "cosine(z1, c)",
            positions = PositionList(Position(zoom = 6.0)),
            radius = 3e2f,
            goldFeature = true,
            slowDualFloat = true
        )
        val hyperbolicSine = Shape(
            id = nextId,
            nameId = R.string.hyperbolic_sine,
            latexId = R.string.hyperbolic_sine_latex,
            thumbnailId = R.drawable.hyperbolicsine_icon,
            loop = "hyperbolic_sine(z1, c)",
            positions = PositionList(Position(zoom = 8.0)),
            radius = 3e2f,
            goldFeature = true,
            slowDualFloat = true
        )
        val hyperbolicCosine = Shape(
            id = nextId,
            nameId = R.string.hyperbolic_cosine,
            latexId = R.string.hyperbolic_cosine_latex,
            thumbnailId = R.drawable.hyperboliccosine_icon,
            loop = "hyperbolic_cosine(z1, c)",
            positions = PositionList(Position(x = -0.075, zoom = 8.0)),
            radius = 3e2f,
            goldFeature = true,
            slowDualFloat = true
        )
        val sine2 = Shape(
            id = nextId,
            nameId = R.string.sine2,
            thumbnailId = R.drawable.sine2_icon,
            loop = "sine2(z1, c)",
            positions = PositionList(Position(zoom = 3.5)),
            radius = 1e1f,
            params = ParamSet(

                listOf(ComplexParam(u = -0.26282884)),
                seed = ComplexParam(u = 1.0)

            ),
            slowDualFloat = true
        )
        val horseshoeCrab = Shape(
            id = nextId,
            nameId = R.string.horseshoe_crab,
            thumbnailId = R.drawable.horseshoecrab_icon,
            loop = "horseshoe_crab(z1, c)",
            positions = PositionList(Position(x = -0.25, zoom = 6.0, rotation = 90.0.inRadians())),
            radius = 3e2f,
            params = ParamSet(

                listOf(ComplexParam(u = sqrt(2.0))),
                seed = ComplexParam(u = 1.0)

            ),
            slowDualFloat = true
        )
        val necklace = Shape(
            id = nextId,
            nameId = R.string.necklace,
            thumbnailId = R.drawable.newton1_icon,
            loop = "necklace(z1, c)",
            params = ParamSet(

                listOf(RealParam(R.string.power, 4.0, Range(3.0, 6.0), discrete = true)),
                julia = ComplexParam(u = 1.0)

            ),
            juliaMode = true,
            isConvergent = true,
            goldFeature = true
        )
        val kleinian = Shape(
            id = nextId,
            nameId = R.string.kleinian,
            thumbnailId = R.drawable.kleinian_icon,
            init = "float K = 0.0; float M = 0.0; vec2 t = vec2(0.0); float k = 0.0; kleinian_init(z, c, alpha, t, K, M, k);",
            loop = "kleinian(z1, c, t, K, M, k)",
            conditional = "kleinian_exit(z, t)",
            delta1 = "kleinian_delta(alpha, z1, t, K, M, k)",
            deltaJulia1 = "kleinian_delta(alpha, z1, t, K, M, k)",
            alphaSeed = Complex.ONE,
            hasAnalyticDelta = true,
            positions = PositionList(julia = Position(y = -0.5, zoom = 1.5)),
            juliaMode = true,
            params = ParamSet(
                listOf(
                    ComplexParam(u = 2.0, goldFeature = true),
                    RealParam(
                        u = 0.0,
                        uRange = Range(0.0, 8.0),
                        discrete = true,
                        devFeature = true
                    ),
                    RealParam(u = 0.0, uRange = Range(0.0, 10.0), devFeature = true),
                    RealParam(u = 0.0, uRange = Range(0.0, 10.0), devFeature = true)
                ),
                julia = ComplexParam(u = 0.0, v = -1.0)
            ),
            maxIter = 128,
            radius = 1e5f,
            compatTextures = Texture.kleinianCompat,
            randomConfigs = listOf(
                Config(
                    Position(x = -0.4295334290133643, y = 0.38494234879699085, zoom = 5.744420e-06),
                    ParamSetPreset(listOf(ComplexParam(1.73151, 0.99999)), julia = ComplexParam())
                ),
                Config(
                    Position(x = -0.009630036767539457, y = -0.5011858994458488, zoom = 8e-9),
                    ParamSetPreset(listOf(ComplexParam(1.909995, 0.05262)), julia = ComplexParam(0.514563, 0.899654))
                ),
                Config(
                    Position(x = -0.40493594951770795, y = 0.20325747432894947, zoom = 3.776192e-11),
                    ParamSetPreset(listOf(ComplexParam(1.9459072820911796, 0.017073582015094166)), julia = ComplexParam(0.0, 0.0))
                ),
                Config(
                    Position(x = -1.0200585296639006, y = -0.897182892791748, zoom = 1.333288e-11),
                    ParamSetPreset(listOf(ComplexParam(1.9779836461344935, -0.007116496722606856)), julia = ComplexParam(0.6343119464506043, 0.3188203189283527))
                ),
                Config(
                    params = ParamSetPreset(listOf(ComplexParam(1.924498781195318, -2.0423778905176144)), julia = ComplexParam(1.0337910391115537, 0.9478327148107198))
                ),
                Config(
                    Position(x = -0.3459687064927218, y = -0.8608266772314501, zoom = 8.906141e-11),
                    ParamSetPreset(julia = ComplexParam(0.0, -1.0))
                ),
                Config(
                    Position(x = -0.11196390685029876, y = 2.6055196122704145, zoom = 8.288150e+00),
                    ParamSetPreset(julia = ComplexParam(-0.022171031104193793, -0.18161116869021693))
                ),
                Config(
                    Position(x = -1.4310397278079345, y = 1.316400098773796, zoom = 3.072387e-11),
                    ParamSetPreset(julia = ComplexParam(0.4800157211367434, 1.3550370479722897))
                ),
                Config(
                    Position(x = -1.0, y = 1.0, zoom = 1.996500e+00),
                    ParamSetPreset(julia = ComplexParam(0.0, 0.0))
                ),
                Config(
                    Position(x = 1.8556128238528031, y = -1.3864689025885142, zoom = 2.286093e-03),
                    ParamSetPreset(julia = ComplexParam(1.1241549801713187, 0.49314517226898524))
                )
            )
        )
        val nova1 = Shape(
            id = nextId,
            nameId = R.string.nova1,
            thumbnailId = R.drawable.nova1_icon,
            loop = "nova1(z1, c)",
            positions = PositionList(Position(x = -0.3, zoom = 1.75, rotation = 90.0.inRadians())),
            params = ParamSet(
                listOf(ComplexParam(u = 1.0, v = 0.0)),
                seed = ComplexParam(u = 1.0)
            ),
            randomConfigs = listOf(
                Config(Position(x = -0.46015983911098746, y = -0.42348638614565837, zoom = 9.796340e-12)),
                Config(Position(x = -0.21810951603343412, y = -0.37576446076679965, zoom = 2.097003e-12)),
                Config(Position(x = -0.39908704718654925, y = -0.08744452476394932, zoom = 6.514582e-12)),
                Config(Position(x = -0.38064677449758333, y = 0.2001213725240039, zoom = 1.427728e-11)),
                Config(Position(x = -0.5635896649723118, y = -0.03954297759331993, zoom = 3.401088e-11)),
                Config(Position(x = -0.8557812281063364, y = -0.0018477970702720802, zoom = 8.837389e-12)),
                Config(Position(x = -1.4959323756432756, y = 1.793829299149963E-14, zoom = 7.494848e-12)),
                Config(Position(x = -0.371262597444334, y = 0.054975906011873465, zoom = 6.085937e-12)),
                Config(Position(x = -0.6145268010352285, y = 0.0051977224302688415, zoom = 1.944551e-12)),
                Config(Position(x = 0.2813725192569744, y = -0.5237614317989664, zoom = 3.946614e-12)),
                Config(Position(x = -0.5803543969710381, y = 3.842453004961987E-10, zoom = 1.142659e-08)),
                Config(
                    Position(x = -0.8853593891739883, y = -0.01965206654301938, zoom = 2.207739e-11),
                    ParamSetPreset(listOf(ComplexParam(2.3219006090867054, 0.0)))
                ),
                Config(
                    Position(x = -0.990709731999416, y = -0.06312351963106129, zoom = 5.303776e-12),
                    ParamSetPreset(listOf(ComplexParam(2.3219006090867054, 0.0)))
                ),
                Config(
                    Position(x = 0.4200203999739625, y = -0.8706026346680269, zoom = 5.667863e-12),
                    ParamSetPreset(listOf(ComplexParam(1.924577601216374, 0.0)))
                ),
                Config(
                    Position(x = -0.702093067881166, y = -1.1600750804641026E-6, zoom = 4.003400e-12),
                    ParamSetPreset(listOf(ComplexParam(1.958477, 0.0)))
                ),
                Config(
                    Position(x = -0.1392320687216921, y = 0.29233188744620875, zoom = 7.104238e-12),
                    ParamSetPreset(listOf(ComplexParam(0.5, 0.0)))
                ),
                Config(
                    Position(x = 0.08015295450658448, y = -0.03384646221266222, zoom = 1.571969e-11),
                    ParamSetPreset(listOf(ComplexParam(0.2447113686689068, 0.45447331019161086)))
                ),
                Config(
                    Position(x = -0.21213081331970926, y = -0.25666467732656684, zoom = 7.002158e-12),
                    ParamSetPreset(listOf(ComplexParam(0.3292794110573584, 1.045805844418405)))
                ),
                Config(
                    Position(x = -0.40418886782002367, y = 0.41521028878801336, zoom = 4.022640e-11),
                    ParamSetPreset(listOf(ComplexParam(1.4402003412345934, -0.8893612210577939)))
                ),
                Config(
                    Position(x = -0.8324816995748711, y = 0.04580649996758555, zoom = 8.237480e-10),
                    ParamSetPreset(listOf(ComplexParam(2.374566526477098, 0.0)))
                ),
                Config(
                    Position(x = -1.1631843227194907, y = -0.45853092531281286, zoom = 3.098953e-10),
                    ParamSetPreset(listOf(ComplexParam(1.8311490108502189, -1.9364171637632333)))
                )
            ),
            isConvergent = true
        )
        val nova2 = Shape(
            id = nextId,
            nameId = R.string.nova2,
            thumbnailId = R.drawable.nova2_icon,
            loop = "nova2(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(x = -0.3, zoom = 5.0)),
            isConvergent = true,
            slowDualFloat = true
        )
        val collatz = Shape(
            id = nextId,
            nameId = R.string.collatz,
            latexId = R.string.collatz_latex,
            thumbnailId = R.drawable.collatz_icon,
            loop = "collatz(z1, c)",
            positions = PositionList(
                Position(zoom = 1.5, rotation = (-90.0).inRadians()),
                Position(rotation = (-90.0).inRadians())
            ),
            radius = 5e1f,
            goldFeature = true,
            slowDualFloat = true
        )
        val mandelex = Shape(
            id = nextId,
            nameId = R.string.mandelex,
            thumbnailId = R.drawable.mandelex_icon,
            loop = "mandelex(z1, c)",
            juliaSeed = true,
            positions = PositionList(Position(zoom = 2e1)),
            params = ParamSet(
                listOf(

                    RealParam(R.string.angle, 180.0, Range(0.0, 360.0), toRadians = true),
                    RealParam(R.string.radius, 0.5, Range(0.0, 1.0)),
                    RealParam(R.string.scale, 2.0, Range(0.0, 5.0)),
                    RealParam(R.string.linear, 2.0, Range(0.0, 5.0))

                )
            ),
            randomConfigs = listOf(
                Config(Position(x = -6.960287095727557,     y = -6.960286426764271,     zoom = 6.119518e-12)),
                Config(Position(x = -3.03022322482586,      y =  2.93476256811757,      zoom = 8.378572e-12)),
                Config(Position(x =  1.4941519031989912,    y = -1.4941323610712551,    zoom = 1.833954e-10)),
                Config(Position(x = -0.5295574849476062,    y = -0.5295573504057748,    zoom = 3.443906e-12)),
                Config(Position(x = -1.0987296817061862,    y =  0.14849113053490653,   zoom = 6.898570e-12)),
                Config(Position(x =  4.9330166446658135,    y =  5.011301065060842,     zoom = 1.313580e-11)),
                Config(Position(x = -5.395944301550589,     y = -5.395907146020218,     zoom = 1.905562e-11)),
                Config(Position(x =  0.8538891531304162,    y =  0.4311078240370171,    zoom = 9.862277e-12)),
                Config(Position(x =  0.005276363696944167,  y = -0.0053181146196019735, zoom = 1.741672e-12)),
                Config(Position(x = -1.1682522068194396,    y = -1.1688282417577152,    zoom = 2.710513e-11)),
                Config(Position(x = -4.965710994099813,     y = -3.995034274312582,     zoom = 3.319648e-11)),
                Config(
                    Position(x = 3.1401307160726186, y = -7.615015172625677, zoom = 1.327730e-11),
                    ParamSetPreset(listOf(RealParam(90.0), RealParam(0.9004), RealParam(2.6915), RealParam(2.0)))
                ),
                Config(
                    Position(x = -1.8255516708971684, y = -2.6313647622123644, zoom = 3.094190e-11),
                    ParamSetPreset(listOf(RealParam(180.0), RealParam(0.7368831332155881), RealParam(2.018705489143493), RealParam(2.0)))
                ),
                Config(
                    Position(x = 3.7855125861787444, y = 4.484628229312334, zoom = 6.097830e-11),
                    ParamSetPreset(listOf(RealParam(143.71428571428578), RealParam(2.2880172157823715), RealParam(2.018705489143493), RealParam(2.0)))
                ),
                Config(
                    Position(x = 11.910201913822725, y = -11.910986698611145, zoom = 1.650693e-11),
                    ParamSetPreset(listOf(RealParam(0.0), RealParam(0.8994047619047618), RealParam(2.9536414978996155), RealParam(2.0)))
                ),
                Config(
                    Position(x = 2.2890042393939845, y = 1.2263527351926713, zoom = 1.135563e-11),
                    ParamSetPreset(listOf(RealParam(-90.0), RealParam(0.5618673696009275), RealParam(5.001929980628484), RealParam(0.78316461472284)))
                ),
                Config(
                    Position(x = -3.558409996640056, y = -0.7136680009617388, zoom = 5.880083e-10),
                    ParamSetPreset(listOf(RealParam(-267.62945761373055), RealParam(1.6395304979104122), RealParam(1.981802244310512), RealParam(0.538823324536518)))
                ),
                Config(
                    Position(x = -2.56822851139012, y = 3.5595011320939465, zoom = 6.354847e-12),
                    ParamSetPreset(listOf(RealParam(-252.13997614735032), RealParam(2.2680292680595944), RealParam(1.759270668506847), RealParam(1.5250321645585287)))
                ),
                Config(
                    Position(x = 0.0, y = 0.0, zoom = 9.342889e+00),
                    ParamSetPreset(listOf(RealParam(0.0), RealParam(3.465478887287552), RealParam(1.8603078756882687), RealParam(3.74659093221028))),
                    juliaMode = true
                ),
                Config(
                    Position(x = -5.581201612433424, y = 2.610737181840405, zoom = 1.547505e-11),
                    ParamSetPreset(listOf(RealParam(0.0), RealParam(4.411387950134202), RealParam(1.5858546838918683), RealParam(3.353578537229507))),
                    juliaMode = true
                ),
                Config(
                    Position(x = 0.4393495033457827, y = -1.2802648302989683, zoom = 1.729945e-11),
                    ParamSetPreset(listOf(RealParam(75.09077400801611), RealParam(4.411387950134202), RealParam(1.139868685550744), RealParam(6.227592286609469)))
                )
            ),
            goldFeature = true
        )
        val cactus = Shape(
            id = nextId,
            nameId = R.string.cactus,
            thumbnailId = R.drawable.cactus_icon,
            loop = "cactus(z1, c)",
            positions = PositionList(Position(rotation = 90.0.inRadians(), zoom = 1.5)),
            randomConfigs = listOf(
                Config(Position(x = 0.6945093055378344, y = -0.12861845070181296, zoom = 1.858179e-10)),
                Config(Position(x = 0.006225699131387254, y = -0.2537841462503991, zoom = 4.841887e-12)),
                Config(Position(x = 0.6655032872777346, y = -0.032189121729864864, zoom = 4.667102e-12)),
                Config(Position(x = 0.6714811656175155, y = 0.1488396121236444, zoom = 4.689878e-11)),
                Config(Position(x = 0.024224252335378785, y = -0.15666749357862203, zoom = 1.604621e-10)),
                Config(Position(x = 0.15788843010762385, y = -0.34606416607416635, zoom = 2.806793e-11)),
                Config(Position(x = 0.6924665168780963, y = -0.14139325157676505, zoom = 1.351636e-11)),

            ),
            juliaSeed = true,
            goldFeature = true
        )
        val sierpinksiTriangle = Shape(
            id = nextId,
            nameId = R.string.sierpinski_triangle,
            thumbnailId = R.drawable.sierpinskitri_icon,
            loop = "sierpinski_tri(z1, c)",
            params = ParamSet(
                listOf(

                    RealParam(R.string.scale, 1.0, Range(2.0 / 3.0, 2.0)),
                    RealParam(R.string.rotate, 0.0, Range(0.0, 360.0), toRadians = true)

                )
            ),
            conditional = "escape_tri(z)",
            juliaMode = true,
            compatTextures = Texture.divergent.minus(Texture.escapeWithOutline),
            goldFeature = true
        )
        val sierpinksiSquare = Shape(
            id = nextId,
            nameId = R.string.sierpinski_square,
            thumbnailId = R.drawable.sierpinskisqr_icon,
            loop = "sierpinski_sqr(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(zoom = 1.75)),
            conditional = "escape_sqr(z)",
            compatTextures = Texture.divergent.minus(Texture.escapeWithOutline),
            goldFeature = true
        )
        val sierpinskiPentagon = Shape(
            id = nextId,
            nameId = R.string.sierpinski_pentagon,
            thumbnailId = R.drawable.sierpinskipent_icon,
            loop = "sierpinski_pent(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(rotation = Math.PI, zoom = 7.0)),
            params = ParamSet(
                listOf(

                    RealParam(R.string.center, 0.0, Range(0.0, 1.0)),
                    RealParam(R.string.scale, 2.618033988, Range(0.0, 10.0)),
                    RealParam(R.string.rotate, 0.0, Range(0.0, 72.0), toRadians = true)

                )
            ),
            conditional = "escape_pent(z)",
            compatTextures = Texture.divergent.minus(Texture.escapeWithOutline),
            goldFeature = true
        )
        val tsquare = Shape(
            id = nextId,
            nameId = R.string.tsquare,
            thumbnailId = R.drawable.tsquare_icon,
            loop = "tsquare(z1, c)",
            juliaMode = true,
            params = ParamSet(
                listOf(

                    RealParam(R.string.shift, 0.5, Range(0.0, 5.0)),
                    RealParam(R.string.scale, 2.0, Range(0.0, 5.0)),
                    RealParam(R.string.rotate, 0.0, Range(0.0, 90.0), toRadians = true)

                )
            ),
            conditional = "escape_sqr(z)",
            compatTextures = Texture.divergent.minus(Texture.escapeWithOutline),
            goldFeature = true
        )
        val lambertNewton = Shape(
            id = nextId,
            nameId = R.string.lambert_newton,
            thumbnailId = R.drawable.lambertnewton_icon,
            positions = PositionList(Position(zoom = 3.5, rotation = 90.0.inRadians())),
            loop = "lambert_newton(z1, c)",
            juliaSeed = true,
            isConvergent = true,
            goldFeature = true,
            slowDualFloat = true
        )
        val taurus = Shape(
            id = nextId,
            nameId = R.string.taurus,
            thumbnailId = R.drawable.taurus_icon,
            loop = "taurus(z1, c)",
            positions = PositionList(Position(x = 1.0, zoom = 7.5, rotation = (90.0).inRadians())),
            isConvergent = true,
            goldFeature = true
        )
        val ammonite = Shape(
            id = nextId,
            nameId = R.string.ammonite,
            thumbnailId = R.drawable.ammonite_icon,
            loop = "ammonite(z1, c)",
            positions = PositionList(
                julia = Position(
                    x = 0.56767345,
                    zoom = 6.0,
                    rotation = 90.0.inRadians()
                )
            ),
            params = ParamSet(julia = ComplexParam(u = -0.56767345)),
            juliaMode = true,
            goldFeature = true
        )
        val angelbrot = Shape(
            id = nextId,
            nameId = R.string.angelbrot,
            thumbnailId = R.drawable.angelbrot_icon,
            loop = "ballfold1(z1, c)",
            positions = PositionList(Position(x = 1.0, zoom = 3.5, rotation = 0.5 * Math.PI)),
            params = ParamSet(listOf(ComplexParam(u = -0.5 * Math.PI))),
            randomConfigs = listOf(
                Config(Position(x = 2.017162690560745, y = -0.4786537794853822, zoom = 1.322218e-11)),
                Config(Position(x = 0.9773544838002148, y = -0.6614610320348585, zoom = 3.759445e-10)),
                Config(Position(x = 0.9253201188114154, y = 0.6015059128438148, zoom = 2.108921e-09)),
                Config(Position(x = 1.9813130085364303, y = -0.02501601357702403, zoom = 2.483103e-12)),
                Config(Position(x = 1.9427528913462402, y = 0.07741398274910632, zoom = 7.463042e-11)),
                Config(Position(x = 0.3575133393298582, y = -8.883119311314569E-4, zoom = 2.722307e-12)),
                Config(Position(x = 0.8243120131295142, y = 0.08864627976382224, zoom = 3.855599e-11)),
                Config(Position(x = 0.9004059035595062, y = -0.4012812697281399, zoom = 2.264883e-12)),
                Config(Position(x = 1.8268827619140546, y = -0.7830913994047538, zoom = 1.034188e-10)),
                Config(Position(x = 2.2053459155151574, y = -0.007555871481825889, zoom = 1.581966e-09)),
                Config(Position(x = 1.7292746158477477, y = -0.6714264832178357, zoom = 3.466081e-11)),
                Config(
                    Position(x = 0.05580714015205857, y = -4.137837271872851E-4, zoom = 1.337604e-12),
                    ParamSetPreset(listOf(ComplexParam(-1.0, 0.0)))
                ),
                Config(
                    Position(x = 1.7731364788030715, y = -0.1427640221234471, zoom = 1.803921e-11),
                    ParamSetPreset(listOf(ComplexParam(-1.0, 0.0)))
                ),
                Config(
                    Position(x = 0.5767410302936397, y = -0.8604288090685704, zoom = 2.528129e-12),
                    ParamSetPreset(listOf(ComplexParam(-1.0, 0.0)))
                ),
                Config(
                    Position(x = 1.2560238239734647, y = 0.04260627689908989, zoom = 7.338126e-12),
                    ParamSetPreset(listOf(ComplexParam(-0.28899446662017886, 0.0)))
                ),
                Config(
                    Position(x = 0.5389487234416973, y = 0.16244658338464807, zoom = 6.100112e-08),
                    ParamSetPreset(listOf(ComplexParam(-0.28899446662017886, 0.0)))
                ),
                Config(
                    Position(x = 1.5259379858094588, y = -0.015740730520604703, zoom = 8.070515e-11),
                    ParamSetPreset(listOf(ComplexParam(-2.897128864888243, 0.0)))
                ),
                Config(
                    Position(x = 0.36720587790287806, y = -2.057492661805755, zoom = 1.438541e-12),
                    ParamSetPreset(listOf(ComplexParam(0.0, 1.694288314693541)))
                ),
                Config(
                    Position(x = 0.10568264520889685, y = -0.4848264941427876, zoom = 2.811789e-12),
                    ParamSetPreset(listOf(ComplexParam(0.0, 0.7917091697036017)))
                ),
                Config(
                    Position(x = -0.3170184710060186, y = -1.7994409853452795, zoom = 1.003468e-11),
                    ParamSetPreset(listOf(ComplexParam(-0.1379855383865396, 1.2276263480016616)))
                ),
                Config(
                    Position(x = -1.3727436870035985, y = -0.9750377677932686, zoom = 4.777768e-11),
                    ParamSetPreset(listOf(ComplexParam(0.5989173546643259, 0.866786877963904)))
                ),
                Config(
                    Position(x = -1.1344499412310234, y = -0.07618975730758917, zoom = 7.582583e-11),
                    ParamSetPreset(listOf(ComplexParam(0.5989173546643259, 0.866786877963904)))
                ),
                Config(
                    Position(x = -1.319783225122982, y = -0.9039508491696058, zoom = 2.234039e-12),
                    ParamSetPreset(listOf(ComplexParam(0.546985470508824, 1.1364850268845008)))
                ),
                Config(
                    Position(x = -0.5111833240882112, y = -1.8796876409380947, zoom = 1.750683e-09),
                    ParamSetPreset(listOf(ComplexParam(0.14232607775513614, 0.332473051890901)))
                ),
                Config(
                    Position(x = 0.32351046993017474, y = 0.6503777490962057, zoom = 6.256495e-12),
                    ParamSetPreset(listOf(ComplexParam(0.14232607775513606, -0.736086576656812)))
                )
            )
        )
        val harriss = Shape(
            id = nextId,
            nameId = R.string.hardware,
            juliaMode = true,
            loop = "harriss(z1, c)",
            params = ParamSet(
                listOf(
                    RealParam(u = 1.0, uRange = Range(0.0, 5.0)),
                    RealParam(u = 1.0, uRange = Range(0.0, 5.0))
                )
            ),
            conditional = "converge_sqr(z)",
            devFeature = true
        )
        val tree = Shape(
            id = nextId,
            nameId = R.string.about,
            juliaMode = true,
            loop = "tree(z1, c)",
            params = ParamSet(
                listOf(
                    ComplexParam(v = 1.0),
                    ComplexParam(v = 0.75),
                    ComplexParam(1.0, 1.0),
                    ComplexParam(v = 0.5)
                )
            ),
            conditional = "converge_sqr(z)",
            devFeature = true
        )
        val tree2 = Shape(
            id = nextId,
            nameId = R.string.about,
            juliaMode = true,
            loop = "tree(z1, c)",
            params = ParamSet(
                listOf(
                    ComplexParam(v = 1.0),
                    ComplexParam(v = 0.75),
                    ComplexParam(1.0, 1.0),
                    ComplexParam(v = 0.5)
                )
            ),
            conditional = "converge_sqr2(z)",
            devFeature = true
        )


        val kaliSquare = Shape(
            R.string.kali_square,
            loop = "kali_square(z1, c)",
            juliaMode = true,
            positions = PositionList(julia = Position(zoom = 4.0)),
            radius = 4e0f
        )
        val mandelbar = Shape(
            R.string.mandelbar,
            loop = "mandelbar(z1, c)",
            positions = PositionList(Position(zoom = 3.0))
        )
        val logistic = Shape(
            R.string.empty,
            latexId = R.string.logistic_katex,
            loop = "",
            positions = PositionList(Position(zoom = 3.5))
            // seed = Complex(0.5, 0.0)
        )
        val bessel = Shape(
            R.string.bessel,
            thumbnailId = R.drawable.bessel_icon,
            loop = "bessel(z1, c)",
            positions = PositionList(Position(zoom = 15.0)),
            radius = 3e2f,
            goldFeature = true,
            slowDualFloat = true
        )
        val newton2 = Shape(
            R.string.empty,
            loop = "",
            positions = PositionList(julia = Position(zoom = 3.5)),
            juliaMode = true,
            params = ParamSet(
                listOf(
                    ComplexParam(u = 1.0, v = 1.0),
                    ComplexParam(u = -1.0, v = -1.0),
                    ComplexParam(u = 2.0, v = -0.5)
                )
            ),
            isConvergent = true
        )
        val newton3 = Shape(
            R.string.empty,
            latexId = R.string.newton3_katex,
            loop = "",
            positions = PositionList(julia = Position(zoom = 5.0)),
            juliaMode = true,
            isConvergent = true
        )
        val persianRug = Shape(
            R.string.empty,
            latexId = R.string.persianrug_katex,
            loop = "",
            juliaSeed = true,
            positions = PositionList(Position(zoom = 1.5)),
            params = ParamSet(listOf(ComplexParam(u = 0.642, v = 0.0))),
            radius = 1e1f,
            isTranscendental = true
        )
        val sine3 = Shape(
            R.string.empty,
            latexId = R.string.sine3_katex,
            loop = "",
            positions = PositionList(Position(zoom = 3.5)),
            params = ParamSet(
                listOf(ComplexParam(u = 0.31960705187983646, vLocked = true)),
                seed = ComplexParam(u = 1.0)
            ),
            radius = 1e1f,
            isTranscendental = true
        )
        val binet = Shape(
            nameId = R.string.binet,
            loop = "binet(z1, c)",
            positions = PositionList(Position(zoom = 5.0)),
            goldFeature = true,
            slowDualFloat = true
        )
        val dragon = Shape(
            nameId = R.string.heighway_dragon,
            loop = "dragon(z1, c)",
            params = ParamSet(
                listOf(

                    ComplexParam(u = 0.5, v = sqrt(2.0))

                )
            ),
            conditional = "n == maxIter - 1u && z.y < z.x && z.y < 1.0 - z.x && z.y > 0.0",
            goldFeature = true
        )
        val cephalopod = Shape(
            nameId = R.string.cephalopod,
            loop = "cephalopod(z1, c)",
            isConvergent = true,
            goldFeature = true
        )
        val phoenix = Shape(
            nameId = R.string.shape_name,
            loop = "phoenix(z1, z2, c)",
            params = ParamSet(
                listOf(
                    RealParam(u = 2.0, uRange = Range(-6.0, 6.0), discrete = true),
                    RealParam(u = 0.0, uRange = Range(-6.0, 6.0), discrete = true),
                    ComplexParam(u = 1.0)
                )
            ),
            goldFeature = true
        )


        var nextCustomShapeNum = 1
        val custom = arrayListOf<Shape>()
        val default = arrayListOf(
            mandelbrot,
            mandelbrotCubic,
            mandelbrotQuartic,
            mandelbrotQuintic,
            mandelbrotSextic,
            mandelbrotPow,
            kleinian,
            mandelex,
            collatz,
            angelbrot,
            clover,
            burningShip,
            burningShipPow,
            mandelbox,
            cactus,
            tsquare,
            sierpinksiTriangle,
            sierpinksiSquare,
            sierpinskiPentagon,
            kali,
            sine,
            cosine,
            hyperbolicSine,
            hyperbolicCosine,
            lambertNewton,
            necklace,
            magnet1,
            magnet2,
            taurus,
            ammonite,
            sine2,
            horseshoeCrab,
            nova1,
            nova2,
            tree,
            tree2,
            harriss
        ).filter { BuildConfig.DEV_VERSION || !it.devFeature }
        val all = ArrayList(default)

    }


    var position = if (juliaMode) positions.julia else positions.main

    val conditional = if (isConvergent) CONVERGE else conditional
    val radius = if (isConvergent) 1e-8f else radius
    val compatTextures = if (isConvergent) Texture.convergent else compatTextures

    val juliaModeInit = juliaMode
    var juliaMode = juliaModeInit
        set(value) {
            field = value
            numParamsInUse = params.size + if (juliaMode) 1 else 0
            if (hasAnalyticDelta) alphaSeed = if (juliaMode) Complex.ONE else Complex.ZERO
            position = if (value) positions.julia else positions.main
        }

    var numParamsInUse = params.size + if (juliaMode) 1 else 0

    init {
        if (!hasAnalyticDelta) {
            delta1 = "delta(_float(z), ${
                loop.replace(
                    "z1, c",
                    "_float(z1) + vec2(0.0001, 0.0), _float(c)"
                )
            }, alpha)"
            alphaSeed = Complex.ONE
        }
        if (!hasCustomId && id != -1) id = Integer.MAX_VALUE - id
    }

    fun initialize(res: Resources) {

        when {
            nameId == -1 && name == "" -> {
                throw Error("neither nameId nor name was passed to the constructor")
            }
            name == "" -> {
                name = res.getString(nameId)
            }
        }

        params.list.forEach { p ->
            if (p.nameId != -1) p.name = res.getString(p.nameId)
        }

        if (hasCustomId) {
            thumbnail = Bitmap.createBitmap(
                Resolution.THUMB.w,
                Resolution.THUMB.w,
                Bitmap.Config.RGB_565
            )
        }

        if (latexId != -1 && latex == "") latex = res.getString(latexId)

    }

    override fun getName(localizedResource: Resources): String {
        return if (isCustom()) name else localizedResource.getString(nameId)
    }

    fun release() {
        thumbnail?.recycle()
        thumbnail = null
    }

    fun setFrom(config: Config) {
        config.juliaMode?.let { juliaMode = it }
        config.position?.let { position.setFrom(it) }
        config.params?.let { params.setFrom(it) }
    }

    fun clone(res: Resources): Shape {
        return Shape(
            name = name + " " + res.getString(R.string.copy),
            latex = latex,
            conditional = conditional,
            init = init,
            loop = CUSTOM_LOOP,
            customLoopSingle = customLoopSingle,
            customLoopDual = customLoopDual,
            final = final,
            delta1 = delta1,
            deltaJulia1 = deltaJulia1,
            alphaSeed = alphaSeed,
            compatTextures = compatTextures.toMutableList(),
            positions = positions.clone(),
            params = params, // clone?
            juliaMode = juliaMode,
            juliaSeed = juliaSeed,
            maxIter = maxIter,
            radius = radius
        )
    }

    fun reset() {

        params.reset()
        positions.reset()
        juliaMode = juliaModeInit

    }

    fun toDatabaseEntity() : ShapeEntity {
        return ShapeEntity(
            if (hasCustomId) id else 0,
            name,
            latex,
            customLoopSingle,
            customLoopDual,
            conditional,
//                positions.default.toData(),
//                positions.julia.toData(),
            positions.main.x,
            positions.main.y,
            positions.main.zoom,
            positions.main.rotation,
            positions.julia.x,
            positions.julia.y,
            positions.julia.zoom,
            positions.julia.rotation,
            juliaMode,
            juliaSeed,
            params.seed.u,
            params.seed.v,
            maxIter,
            radius,
            isConvergent,
            hasDualFloat,
            isFavorite
        )
    }

    fun generateStarredKey(usResources: Resources) : String {
        return "Shape${usResources.getString(nameId).replace(" ", "")}Starred"
    }

    override fun equals(other: Any?): Boolean {
        return other is Shape && id == other.id
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun isCustom(): Boolean = hasCustomId

}