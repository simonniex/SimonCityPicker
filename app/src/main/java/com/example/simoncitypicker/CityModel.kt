package com.example.simoncitypicker


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Area(val areaCode: String, val areaName: String)

@Serializable
data class City(
    @SerialName("cityCode") val cityCode: String,
    @SerialName("cityName") val name: String,
    @SerialName("mallAreaList") val districts: List<Area> // 确保与JSON中的键相匹配
)

@Serializable
data class Province(
    @SerialName("provinceCode") val provinceCode: String,
    @SerialName("provinceName") val name: String,
    @SerialName("mallCityList") val cities: List<City> // 这里应该是mallCityList而不是mallAreaList
)