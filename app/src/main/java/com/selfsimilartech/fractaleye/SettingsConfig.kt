package com.selfsimilartech.fractaleye

object SettingsConfig {
        var goldEnabled                 = false
        var resolution                  = Resolution.R1440
        var aspectRatio                 = AspectRatio.RATIO_SCREEN
        var gpuPrecision                = GpuPrecision.SINGLE
        var cpuPrecision                = CpuPrecision.DOUBLE
        var perturbPrecision            = 64L
        var hardwareProfile             = HardwareProfile.GPU
        var useAlternateSplit           = false
        var chunkProfile                = ChunkProfile.HIGH
        var autoPrecision               = true
        var continuousPosRender         = false
        var continuousParamRender       = true
        var showProgress                = true
        var sampleOnStrictTranslate     = true
        var renderBackground            = true
        var allowSlowDualfloat          = false
        var fitToViewport               = false
        var hideNavBar                  = true
        var restrictParams              = true
        var colorListViewType           = ListLayoutType.GRID
        var shapeListViewType           = ListLayoutType.GRID
        var textureListViewType         = ListLayoutType.GRID
        var bookmarkListViewType        = ListLayoutType.GRID
        var autofitColorRange           = false
        var zoomSensitivity             = Sensitivity.MED
        var rotationSensitivity         = Sensitivity.MED
        var shiftSensitivity            = Sensitivity.MED
}