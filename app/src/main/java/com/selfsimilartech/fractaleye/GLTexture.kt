package com.selfsimilartech.fractaleye

import android.opengl.GLES30.*
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class GLTexture (
        val res                     : Resolution,
        private val interpolation   : Int,
        private val internalFormat  : Int,
        val index                   : Int,
        val name                    : String
) {

    val id : Int
    private val numComponents : Int
    private val bytesPerComponent : Int
    private val bytesPerTexel : Int
    val type : Int
    val format : Int
    var byteBuffer : ByteBuffer?
    var floatBuffer : FloatBuffer?

    var pixelsPerChunk = 0
    var internalFormatStr = ""

    init {

        // create texture id
        val b = IntBuffer.allocate(1)
        glGenTextures(1, b)
        id = b[0]

        // allocate texture memory
        numComponents = when(internalFormat) {
            GL_R32UI, GL_R16UI              ->  1
            GL_RG16UI, GL_RG32UI, GL_RG32I  ->  2
            GL_RGBA8                        ->  4
            else -> 0
        }
        bytesPerComponent = when (internalFormat) {
            GL_RGBA8                       ->  1
            GL_RG16UI, GL_R16UI            ->  2
            GL_RG32UI, GL_RG32I, GL_R32UI  ->  4
            else -> 0
        }
        bytesPerTexel = numComponents*bytesPerComponent
        internalFormatStr = when(internalFormat) {
            GL_RGBA8 -> "GL_RGBA8"
            GL_R16UI -> "GL_R16UI"
            GL_R32UI -> "GL_R32UI"
            GL_RG16UI -> "GL_RG16UI"
            GL_RG32UI -> "GL_RG32UI"
            else -> "not what u wanted"
        }

        // bind and set texture parameters
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, id)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, interpolation)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, interpolation)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        type = when(internalFormat) {
            GL_RGBA8             -> GL_UNSIGNED_BYTE
            GL_R16UI, GL_RG16UI  -> GL_UNSIGNED_SHORT
            GL_R32UI, GL_RG32UI  -> GL_UNSIGNED_INT
            else -> 0
        }
        format = when(internalFormat) {
            GL_RGBA8              -> GL_RGBA
            GL_RG16UI, GL_RG32UI  -> GL_RG_INTEGER
            GL_R16UI, GL_R32UI    -> GL_RED_INTEGER
            else                  -> GL_RGBA
        }

        byteBuffer = ByteBuffer.allocateDirect(res.w*res.h*bytesPerTexel).order(ByteOrder.nativeOrder())
        floatBuffer = byteBuffer?.asFloatBuffer()
        //Log.e("FSV", "buffer capacity: ${buffer.capacity()}")
        glTexImage2D(
                GL_TEXTURE_2D,              // target
                0,                          // mipmap level
                internalFormat,             // internal format
                res.w, res.h,               // texture resolution
                0,                          // border
                format,                     // internalFormat
                type,                       // type
                floatBuffer                 // memory pointer
        )




    }


    fun get(i: Int, j: Int, k: Int) : Float = floatBuffer?.get(numComponents*(j*res.w + i) + k) ?: 0f
    fun set(i: Int, j: Int, k: Int, value: Float) {
        floatBuffer?.put(numComponents*(j*res.w + i) + k, value)
    }
    fun put(array: FloatArray) {
        floatBuffer?.put(array, 0, array.size)
    }
    fun set(array: FloatArray) {
        floatBuffer?.position(0)
        floatBuffer?.put(array)
        update()
    }
    fun update() {
        floatBuffer?.position(0)
        glActiveTexture(GL_TEXTURE0 + index)
        glBindTexture(GL_TEXTURE_2D, id)
        glTexImage2D(
                GL_TEXTURE_2D,      // target
                0,                  // mipmap level
                internalFormat,     // internal format
                res.w, res.h,       // texture resolution
                0,                  // border
                format,             // internalFormat
                type,               // type
                floatBuffer         // memory pointer
        )
    }
    fun delete() {
        glDeleteTextures(1, intArrayOf(id), 0)
        byteBuffer = null
        floatBuffer = null
        // System.gc()
    }

    override fun toString(): String {
        return "$name -- id: $id, res: (${res.w}, ${res.h}), internalFormat: $internalFormatStr, index: $index, bytesPerTexel: $bytesPerTexel, totalBytes: ${res.w*res.h*bytesPerTexel}"
    }

}