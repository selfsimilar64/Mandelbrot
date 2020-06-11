package com.selfsimilartech.fractaleye

class SettingsConfig (
        var resolution               : Resolution      = Resolution.FULL,
        var gpuPrecision             : GpuPrecision    = GpuPrecision.SINGLE,
        var cpuPrecision             : CpuPrecision    = CpuPrecision.DOUBLE,
        var perturbPrecision         : Long            = 64L,
        var hardwareProfile          : HardwareProfile = HardwareProfile.GPU,
        var deepZoomHardwareProfile  : HardwareProfile = HardwareProfile.GPU,
        var autoPrecision            : Boolean         = true,
        var continuousRender         : Boolean         = false,
        var displayParams            : Boolean         = false,
        var showProgress             : Boolean         = true,
        var sampleOnStrictTranslate  : Boolean         = false,
        var renderBackground         : Boolean         = true,
        var fitToViewport            : Boolean         = false,
        var hideNavBar               : Boolean         = true,
        var showHints                : Boolean         = true,
        var colorListViewType        : ListLayoutType  = ListLayoutType.GRID,
        var shapeListViewType        : ListLayoutType  = ListLayoutType.GRID,
        var textureListViewType      : ListLayoutType  = ListLayoutType.GRID,
        var autofitColorRange        : Boolean         = false
)