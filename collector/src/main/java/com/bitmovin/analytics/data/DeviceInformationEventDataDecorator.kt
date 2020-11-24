package com.bitmovin.analytics.data

open class DeviceInformationEventDataDecorator(
    private val deviceInformationProvider: DeviceInformationProvider
) : EventDataDecorator {
    override fun decorate(data: EventData) {
        val deviceInfo = this.deviceInformationProvider.getDeviceInformation()
        data.userAgent = deviceInfo.userAgent
        data.deviceInformation = DeviceInformationDto(deviceInfo.manufacturer, deviceInfo.model, deviceInfo.isTV)
        data.language = deviceInfo.locale

        data.domain = deviceInfo.packageName
        data.screenHeight = deviceInfo.screenHeight
        data.screenWidth = deviceInfo.screenWidth
        data.platform = if (deviceInfo.isTV) "androidTV" else "android"
    }
}
