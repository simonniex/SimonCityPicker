package com.example.citypicker

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

fun loadJsonFromAssets(context: Context, fileName: String): String? {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (ex: IOException) {
        ex.printStackTrace()
        null
    }
}

fun parseProvinces(jsonString: String): List<Province> {
    return Json.decodeFromString(jsonString)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CityPicker(
    provinces: List<Province>,
    bottomSheetState: ModalBottomSheetState,
    onDismiss: () -> Unit,
    onCitySelected: (String, String, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val provinceListState = rememberLazyListState()
    val cityListState = rememberLazyListState()
    val districtListState = rememberLazyListState()

    var selectedProvinceIndex by remember { mutableStateOf(0) }
    var selectedCityIndex by remember { mutableStateOf(0) }
    var selectedDistrictIndex by remember { mutableStateOf(0) }

    LaunchedEffect(selectedProvinceIndex) {
        coroutineScope.launch {
            cityListState.scrollToItem(0)
            districtListState.scrollToItem(0)
            selectedCityIndex = 0
            selectedDistrictIndex = 0
        }
    }

    LaunchedEffect(selectedCityIndex) {
        coroutineScope.launch {
            districtListState.scrollToItem(0)
            selectedDistrictIndex = 0
        }
    }

    val scope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "请选择城市",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WheelPicker(
                            listState = provinceListState,
                            items = provinces.map { it.name },
                            onItemSelected = { index -> selectedProvinceIndex = index },
                            modifier = Modifier.weight(1f)
                        )
                        WheelPicker(
                            listState = cityListState,
                            items = if (provinces.isNotEmpty() && selectedProvinceIndex < provinces.size)
                                provinces[selectedProvinceIndex].cities.map { it.name }
                            else emptyList(),
                            onItemSelected = { index -> selectedCityIndex = index },
                            modifier = Modifier.weight(1f)
                        )
                        WheelPicker(
                            listState = districtListState,
                            items = if (provinces.isNotEmpty() && selectedProvinceIndex < provinces.size && selectedCityIndex < provinces[selectedProvinceIndex].cities.size)
                                provinces[selectedProvinceIndex].cities[selectedCityIndex].districts.map { it.areaName }
                            else emptyList(),
                            onItemSelected = { index -> selectedDistrictIndex = index },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    androidx.compose.material3.Button(
                        onClick = {
                            if (provinces.isNotEmpty() && selectedProvinceIndex < provinces.size) {
                                val province = provinces[selectedProvinceIndex].name
                                val city = if (selectedCityIndex < provinces[selectedProvinceIndex].cities.size)
                                    provinces[selectedProvinceIndex].cities[selectedCityIndex].name
                                else ""
                                val district = if (selectedCityIndex < provinces[selectedProvinceIndex].cities.size && selectedDistrictIndex < provinces[selectedProvinceIndex].cities[selectedCityIndex].districts.size)
                                    provinces[selectedProvinceIndex].cities[selectedCityIndex].districts[selectedDistrictIndex].areaName
                                else ""
                                onCitySelected(province, city, district)
                            }
                            scope.launch {
                                bottomSheetState.hide()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        androidx.compose.material3.Text(text = "确定")
                    }
                }
            }
        }
    ) {
        LaunchedEffect(Unit) {
            scope.launch {
                bottomSheetState.show()
            }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    listState: LazyListState,
    items: List<String>,
    onItemSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val centerIndex = remember { mutableStateOf(0) }

    Box(
        modifier
            .height(200.dp)
            .width(100.dp)
    ) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 80.dp), // 80.dp 的 padding 用来模拟中间预选区
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, item ->
                Text(
                    text = item,
                    fontSize = 18.sp,
                    fontWeight = if (index == centerIndex.value) FontWeight.Bold else FontWeight.Light,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .animateItemPlacement()
                )
            }
        }

        // 上下两条线，中间透明
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(25.dp) // 与 Box 的高度一致
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0x80000000)) // 半透明上边线
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0x80000000)) // 半透明下边线
                    .align(Alignment.BottomCenter)
            )
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .map { visibleItems ->
                    val viewportStartOffset = listState.layoutInfo.viewportStartOffset
                    val viewportEndOffset = listState.layoutInfo.viewportEndOffset
                    val viewportCenter = (viewportEndOffset - viewportStartOffset) / 2 + viewportStartOffset

                    visibleItems.minByOrNull {
                        Math.abs((it.offset + it.size / 2) - viewportCenter)
                    }
                }
                .collect { centerItem ->
                    centerItem?.let {
                        centerIndex.value = it.index
                        onItemSelected(it.index)
                    }
                }
        }

        LaunchedEffect(listState) {
            listState.interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is DragInteraction.Stop, is DragInteraction.Cancel -> {
                        scope.launch {
                            val visibleItems = listState.layoutInfo.visibleItemsInfo
                            val viewportStartOffset = listState.layoutInfo.viewportStartOffset
                            val viewportEndOffset = listState.layoutInfo.viewportEndOffset
                            val viewportCenter = (viewportEndOffset - viewportStartOffset) / 2 + viewportStartOffset

                            val centerItem = visibleItems.minByOrNull {
                                Math.abs((it.offset + it.size / 2) - viewportCenter)
                            }
                            centerItem?.let {
                                listState.animateScrollToItem(it.index)
                                centerIndex.value = it.index
                                onItemSelected(it.index)
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SimonCityPicker(
    context: Context,
    isCity:Boolean,
    fileName: String = "city.json",
    onCitySelected: (String) -> Unit
) {

    val jsonString = loadJsonFromAssets(context, fileName)
    val provinces: List<Province> = jsonString?.let { parseProvinces(it) } ?: emptyList()
    var selectedCity by remember { mutableStateOf("请选择城市") }
    var isCityState by remember { mutableStateOf(isCity) }
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = if (isCity) ModalBottomSheetValue.Expanded else ModalBottomSheetValue.Hidden
    )

    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.currentValue }
            .collect { value ->
                if (value == ModalBottomSheetValue.Hidden) {
                    onCitySelected(selectedCity)
                }
            }
    }

    if (isCity) {
        CityPicker(
            provinces = provinces,
            bottomSheetState = bottomSheetState,
            onDismiss = { isCityState = false },
            onCitySelected = { province, city, district ->
                selectedCity = "$province+$city+$district"
                onCitySelected(selectedCity)
            }
        )
    }
}


