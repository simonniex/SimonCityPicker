package com.example.simoncitypicker

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlin.math.abs


// 扩展函数优化 JSON 文件加载
fun Context.loadJsonAsset(fileName: String): Flow<String?> = flow {
    emit(assets.open(fileName).bufferedReader().use { it.readText() })
}.catch { exception ->
    exception.printStackTrace()
    emit(null.toString())
}

// 省市数据解析优化
fun parseProvinces(jsonString: String): List<Province> = Json.decodeFromString(jsonString)


fun Modifier.handleVerticalDrag(
    selectedProvinceIndex: Int,
    provinces: List<Province>
): Modifier {
    return this.pointerInput(Unit) {
        detectVerticalDragGestures { change, dragAmount ->
            val isAtTop = selectedProvinceIndex == 0 && dragAmount > 0
            val isAtBottom = selectedProvinceIndex == provinces.lastIndex && dragAmount < 0
            if (isAtTop || isAtBottom) {
                change.consume()
            }
        }
    }
}

private fun LazyListState.isScrolledToEnd(): Boolean {
    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
    val totalItemsCount = layoutInfo.totalItemsCount
    return lastVisibleItemIndex == totalItemsCount - 1 &&
            (layoutInfo.visibleItemsInfo.lastOrNull()?.offset?.plus(layoutInfo.visibleItemsInfo.lastOrNull()?.size ?: 0) ?: 0) >= layoutInfo.viewportEndOffset
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val centerIndex = remember { mutableStateOf(0) }
    val lazyColumnModifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectVerticalDragGestures { change, dragAmount ->
                val isAtTop = listState.firstVisibleItemIndex == 0 && listState.layoutInfo.visibleItemsInfo.firstOrNull()?.offset == 0
                val isAtBottom = listState.isScrolledToEnd() // 使用扩展函数简化逻辑

                if ((isAtTop && dragAmount > 0) || (isAtBottom && dragAmount < 0)) {
                    change.consume()
                }
            }
        }

    Box(
        modifier = modifier
            .height(200.dp)
            .width(100.dp)
    ) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 80.dp),
            modifier = lazyColumnModifier
        ) {
            itemsIndexed(items) { index, item ->
                Text(
                    text = item,
                    fontSize = 18.sp,
                    fontWeight = if (index == centerIndex.value) FontWeight.Bold else FontWeight.Light,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable {
                            coroutineScope.launch {
                                listState.scrollToItem(index)
                                centerIndex.value = index
                                onItemSelected(index)
                            }
                        }
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(25.dp)) {
            Divider(color = Color(0x80000000), thickness = 2.dp)
            Divider(color = Color(0x80000000), thickness = 2.dp, modifier = Modifier.align(Alignment.BottomCenter))
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .filter { !it }
                .collect {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    val viewportCenter = (listState.layoutInfo.viewportEndOffset + listState.layoutInfo.viewportStartOffset) / 2
                    val nearestItem = visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                    nearestItem?.let {
                        coroutineScope.launch {
                            listState.scrollToItem(it.index)
                            centerIndex.value = it.index
                            onItemSelected(it.index)
                        }
                    }

                }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPicker(
    provincesFlow: Flow<List<Province>>,
    onDismiss: () -> Unit,
    onCitySelected: (String, String, String) -> Unit
) {
    val provinces by provincesFlow.collectAsState(initial = emptyList())

    var selectedProvinceIndex by remember { mutableIntStateOf(0) }
    var selectedCityIndex by remember { mutableIntStateOf(0) }
    var selectedDistrictIndex by remember { mutableIntStateOf(0) }

    // 当省变化时重置区的索引
    LaunchedEffect(selectedProvinceIndex) {
        selectedDistrictIndex = 0 // 只重置区
        selectedCityIndex = 0 // 也可以选择是否重置市
    }

    // 当市变化时重置区的索引
    LaunchedEffect(selectedCityIndex) {
        selectedDistrictIndex = 0 // 只重置区
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .handleVerticalDrag(selectedProvinceIndex, provinces)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                onCitySelected("", "", "")
                onDismiss() }
            ) {
                Text("取消")
            }
            Text("请选择城市", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = {
                if (provinces.isNotEmpty()) {
                    val province = provinces[selectedProvinceIndex].name
                    val city = provinces[selectedProvinceIndex].cities.getOrNull(selectedCityIndex)?.name ?: ""
                    val district = provinces[selectedProvinceIndex].cities.getOrNull(selectedCityIndex)?.districts?.getOrNull(selectedDistrictIndex)?.areaName ?: ""
                    onCitySelected(province, city, district)
                }
                onDismiss()
            }) {
                Text("确定")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth()){
            // Header with Cancel and Confirm buttons

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val selectedProvince = provinces.getOrNull(selectedProvinceIndex)
                val selectedCity = selectedProvince?.cities?.getOrNull(selectedCityIndex)

                WheelPicker(
                    items = provinces.map { it.name },
                    onItemSelected = { selectedProvinceIndex = it },
                    modifier = Modifier.weight(1f)
                )
                WheelPicker(
                    items = selectedProvince?.cities?.map { it.name } ?: emptyList(),
                    onItemSelected = { selectedCityIndex = it },
                    modifier = Modifier.weight(1f)
                )
                WheelPicker(
                    items = selectedCity?.districts?.map { it.areaName } ?: emptyList(),
                    onItemSelected = { selectedDistrictIndex = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun SimonCityPicker(
    context: Context,
    isCity: Boolean,
    fileName: String = "city.json",
    onCitySelected: (String) -> Unit
) {
    val provincesFlow = context.loadJsonAsset(fileName)
        .map { jsonString -> jsonString?.let(::parseProvinces) ?: emptyList() }
        .shareIn(rememberCoroutineScope(), SharingStarted.Lazily, replay = 1)

    var isCityVisible by remember { mutableStateOf(isCity) }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isCityVisible) {
        if (!isCityVisible) {
            coroutineScope.launch {
                if (scaffoldState.bottomSheetState.isVisible) {
                    scaffoldState.bottomSheetState.hide()
                }
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            Box(modifier = Modifier.padding(bottom = BottomSheetDefaults.SheetPeekHeight)) { // 增加底部填充
                CityPicker(
                    provincesFlow = provincesFlow,
                    onDismiss = { isCityVisible = false },
                    onCitySelected = { province, city, district ->
                        val selectedCity = "$province+$city+$district"
                        onCitySelected(selectedCity)
                        isCityVisible = false
                    }
                )
            }
        },
        sheetPeekHeight = BottomSheetDefaults.SheetPeekHeight,
        sheetSwipeEnabled = true,
    ) {
        if (isCityVisible) {
            LaunchedEffect(Unit) {
                coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
            }
        }
    }
}
