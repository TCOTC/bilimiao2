package cn.a10miaomiao.bilimiao.compose.comm.mypage

import android.content.Context
import android.view.View
import androidx.compose.runtime.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.MyPageMenu
import com.a10miaomiao.bilimiao.comm.mypage.SearchConfigInfo

private var _configId = 0
interface OnMyPageListener {
    fun onMenuItemClick(view: View, menuItem: MenuItemPropInfo)
    fun onSearchSelfPage(context: Context, keyword: String)
}

class PageConfigInfo(
    val page: MyPage
) {
    private val configList = mutableListOf<Cofing>()
    private val listenerMap = mutableMapOf<Int, OnMyPageListener>()

    fun addConfig(id: Int, configBuilder: (Cofing) -> Unit) {
        configList.add(Cofing(id).apply(configBuilder))
    }

    fun removeConfig(id: Int) {
        val i = configList.indexOfFirst { it.id == id }
        if (i != -1) {
            configList.removeAt(i)
        }
    }

    fun lastConfig() = configList.lastOrNull()

    fun putMyPageListener(id: Int, listener: OnMyPageListener) {
        listenerMap[id] = listener
    }

    fun removeMyPageListener(id: Int) {
        listenerMap.remove(id)
    }

    fun onMenuItemClick(view: View, menuItem: MenuItemPropInfo) {
        val id = lastConfig()?.id ?: return
        val listener = listenerMap[id] ?: return
        listener.onMenuItemClick(view, menuItem)
    }

    fun onSearchSelfPage(context: Context, keyword: String) {
        val id = lastConfig()?.id ?: return
        val listener = listenerMap[id] ?: return
        listener.onSearchSelfPage(context, keyword)
    }

    class Cofing(
        val id: Int,
    ) {
        var title: String = ""
        var menu: MyPageMenu? = null
        var search: SearchConfigInfo? = null
    }
}

internal val LocalPageConfigInfo: ProvidableCompositionLocal<PageConfigInfo?> =
    compositionLocalOf { null }

@Composable
fun PageConfig(
    title: String = "",
    menu: MyPageMenu? = null,
    search: SearchConfigInfo? = null
): Int {
    val pageConfigInfo = LocalPageConfigInfo.current ?: return -1
    val configId = remember {
        _configId++
    }
    DisposableEffect(
        LocalLifecycleOwner.current.lifecycle,
        title, menu, search
    ) {
        pageConfigInfo.addConfig(configId) {
            it.title = title
            it.menu = menu
            it.search = search
        }
        pageConfigInfo.page.pageConfig.notifyConfigChanged()
        onDispose {
            pageConfigInfo.let {
                it.removeConfig(configId)
                it.page.pageConfig.notifyConfigChanged()
            }
        }
    }
    return configId
}

@Composable
fun PageListener(
    configId: Int,
    onSearchSelfPage: ((keyword: String) -> Unit)? = null,
    onMenuItemClick: ((view: View, menuItem: MenuItemPropInfo) -> Unit)? = null
) {
    val pageConfigInfo = LocalPageConfigInfo.current
    if (configId == -1 || pageConfigInfo == null) {
        return
    }
    DisposableEffect(LocalLifecycleOwner.current,
        configId, onSearchSelfPage, onMenuItemClick) {
        val listener = object : OnMyPageListener {
            override fun onMenuItemClick(view: View, menuItem: MenuItemPropInfo) {
                onMenuItemClick?.invoke(view, menuItem)
            }

            override fun onSearchSelfPage(context: Context, keyword: String) {
                onSearchSelfPage?.invoke(keyword)
            }
        }
        pageConfigInfo.putMyPageListener(configId, listener)
        onDispose {
            pageConfigInfo.removeMyPageListener(configId)
        }
    }
}
