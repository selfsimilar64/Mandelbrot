package com.selfsimilartech.fractaleye

import org.apfloat.Apfloat
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


class Position(

        x: Double = 0.0,
        y: Double = 0.0,
        zoom: Double = 1.0,
        rotation: Double = 0.0,
        xap: Apfloat = Apfloat(x.toString(), 32L),
        yap: Apfloat = Apfloat(y.toString(), 32L),
        var ap: Long = 32L,
        xLocked: Boolean = false,
        yLocked: Boolean = false,
        zoomLocked: Boolean = false,
        rotationLocked: Boolean = false

) {

    constructor(data: Data) : this(x = data.x, y = data.y, zoom = data.zoom, rotation = data.rotation)

    data class Data(
            val x: Double = 0.0,
            val y: Double = 0.0,
            val zoom: Double = 1.0,
            val rotation: Double = 0.0
    )

    private val xInit = x
    private val yInit = y
    private val zoomInit = zoom
    private val rotationInit = rotation
    private val xLockedInit = xLocked
    private val yLockedInit = yLocked
    private val zoomLockedInit = zoomLocked
    private val rotationLockedInit = rotationLocked


    var x = xInit
        set(value) { if (!xLocked) { field = value } }

    var y = yInit
        set(value) { if (!yLocked) { field = value } }

    var zoom = zoomInit
        set(value) { if (!zoomLocked) { field = value } }

    var rotation = rotationInit
        set(value) { if (!rotationLocked) {
            field = when {
                value <= Math.PI -> ((value - Math.PI).rem(2.0 * Math.PI)) + Math.PI
                value > Math.PI -> ((value + Math.PI).rem(2.0 * Math.PI)) - Math.PI
                else -> value
            }

        }}

    var xLocked = xLockedInit
    var yLocked = yLockedInit
    var zoomLocked = zoomLockedInit
    var rotationLocked = rotationLockedInit



    private val xapInit = xap
    private val yapInit = yap
    var xap = xapInit
    var yap = yapInit


    fun clone() : Position {
        return Position(x, y, zoom, rotation)
    }
    fun toData() : Data {
        return Data(x, y, zoom, rotation)
    }

    fun xyOf(u: Float, v: Float) : Pair<Double, Double> {
        // u -- [-1/2, 1/2]
        // v -- [-r/2, r/2]

        val qx = u*zoom
        val qy = v*zoom
        val sinTheta = sin(rotation)
        val cosTheta = cos(rotation)
        val fx = x + qx*cosTheta - qy*sinTheta
        val fy = y + qx*sinTheta + qy*cosTheta
        return Pair(fx, fy)

    }
    fun delta(pos: Position, resolution: Int) : Position {
        return Position(
                (pos.x - x) / resolution,
                (pos.y - y) / resolution,
                (zoom / pos.zoom).pow(1.0/resolution),
                (pos.rotation - rotation) / resolution
        )
    }
    fun setFrom(newPos: Position) {
        val xLockedPrev = xLocked
        val yLockedPrev = yLocked
        val zoomLockedPrev = zoomLocked
        val rotationLockedPrev = rotationLocked
        xLocked = false
        yLocked = false
        zoomLocked = false
        rotationLocked = false
        x = newPos.x
        y = newPos.y
        zoom = newPos.zoom
        rotation = newPos.rotation
        xLocked = xLockedPrev
        yLocked = yLockedPrev
        zoomLocked = zoomLockedPrev
        rotationLocked = rotationLockedPrev
    }

    fun translate(dx: Double, dy: Double) {

        x += dx
        y += dy

    }
    fun translateAp(dx: Apfloat, dy: Apfloat) {

//        xap = xap.add(dx)
//        yap = yap.add(dy)

    }
    fun translate(dx: Float, dy: Float) {  // dx, dy --> [0, 1]

        val tx = dx*zoom
        val ty = dy*zoom
        val sinTheta = sin(-rotation)
        val cosTheta = cos(rotation)
        x -= tx*cosTheta - ty*sinTheta
        y += tx*sinTheta + ty*cosTheta

//        xap = xap.subtract(Apfloat(tx * cosTheta - ty * sinTheta, ap))
//        yap = yap.add(Apfloat(tx * sinTheta + ty * cosTheta, ap))

    }

    fun zoom(dZoom: Float, prop: DoubleArray = doubleArrayOf(0.0, 0.0)) {

        if (!zoomLocked) {

            // unlock x and y to allow auxiliary transformations
            val xLockedTemp = xLocked
            val yLockedTemp = yLocked
            xLocked = false
            yLocked = false

            // calculate scaling variables
            val qx = prop[0] * zoom
            val qy = prop[1] * zoom
            val sinTheta = sin(rotation)
            val cosTheta = cos(rotation)
            val fx = x + qx * cosTheta - qy * sinTheta
            val fy = y + qx * sinTheta + qy * cosTheta

            // scale
            translate(-fx, -fy)
            x /= dZoom
            y /= dZoom
            translate(fx, fy)

//            val fxap = xap.add(Apfloat(qx * cosTheta - qy * sinTheta, ap))
//            val fyap = yap.add(Apfloat(qx * sinTheta + qy * cosTheta, ap))
//            translateAp(fxap.negate(), fyap.negate())
//            xap = xap.divide(Apfloat(dZoom, ap))
//            yap = yap.divide(Apfloat(dZoom, ap))
//            translateAp(fxap, fyap)


            zoom /= dZoom

            // set x and y locks to previous values
            xLocked = xLockedTemp
            yLocked = yLockedTemp

        }

    }
    fun rotate(dTheta: Float, prop: DoubleArray) {

        if (!rotationLocked) {

            // unlock x and y to allow auxiliary transformations
            val xLockedTemp = xLocked
            val yLockedTemp = yLocked
            xLocked = false
            yLocked = false

            // calculate rotation variables
            var qx = prop[0] * zoom
            var qy = prop[1] * zoom
            val sinTheta = sin(rotation)
            val cosTheta = cos(rotation)
            val fx = x + qx * cosTheta - qy * sinTheta
            val fy = y + qx * sinTheta + qy * cosTheta
            val sindTheta = sin(-dTheta)
            val cosdTheta = cos(dTheta)

            // rotate
            translate(-fx, -fy)
            qx = x
            qy = y
            x = qx * cosdTheta - qy * sindTheta
            y = qx * sindTheta + qy * cosdTheta
            translate(fx, fy)


//            qx = prop[0] * zoom
//            qy = prop[1] * zoom
//            val fxap = xap.add(Apfloat(qx * cosTheta - qy * sinTheta, ap))
//            val fyap = yap.add(Apfloat(qx * sinTheta + qy * cosTheta, ap))
//            translateAp(fxap.negate(), fyap.negate())
//            val qxap = xap
//            val qyap = yap
//            val sindThetaAp = Apfloat(sindTheta, ap)
//            val cosdThetaAp = Apfloat(cosdTheta, ap)
//            xap = qxap.multiply(cosdThetaAp).subtract(qyap.multiply(sindThetaAp))
//            yap = qxap.multiply(sindThetaAp).add(qyap.multiply(cosdThetaAp))
//            translateAp(fxap, fyap)


            rotation -= dTheta.toDouble()

            // set x and y locks to previous values
            xLocked = xLockedTemp
            yLocked = yLockedTemp

        }

    }

    fun reset() {
        x = xInit
        y = yInit
        zoom = zoomInit
        rotation = rotationInit
        xLocked = xLockedInit
        yLocked = yLockedInit
        zoomLocked = zoomLockedInit
        rotationLocked = rotationLockedInit
    }
    fun updatePrecision(newPrecision: Long) {

        //Log.e("MAIN ACTIVITY", "new position precision: $newPrecision")
//        ap = newPrecision
//        xap = Apfloat(xap.toString(), ap)
//        yap = Apfloat(yap.toString(), ap)

    }

}