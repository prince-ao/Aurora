package com.funkymuse.aurora.latestBooks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltNavGraphViewModel
import androidx.navigation.NavBackStackEntry
import com.crazylegend.kotlinextensions.log.debug
import com.crazylegend.retrofit.retrofitResult.RetrofitResult
import com.crazylegend.retrofit.retrofitResult.handle
import com.crazylegend.retrofit.retryOnConnectedToInternet
import com.crazylegend.retrofit.throwables.NoConnectionException
import com.funkymuse.aurora.R
import com.funkymuse.aurora.book.Book
import com.funkymuse.aurora.components.ErrorMessage
import com.funkymuse.aurora.components.ErrorWithRetry
import com.funkymuse.aurora.dto.Book
import com.funkymuse.aurora.dto.Mirrors
import com.funkymuse.aurora.extensions.CardListShimmer
import com.funkymuse.aurora.extensions.stateWhenStarted
import com.funkymuse.aurora.internetDetector.InternetDetectorViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.toPaddingValues

/**
 * Created by FunkyMuse on 25/02/21 to long live and prosper !
 */

@Composable
fun LatestBooks(
    navBackStackEntry: NavBackStackEntry,
    onBookClicked: (id: Int, Mirrors) -> Unit
) {
    val internetDetectorVM = hiltNavGraphViewModel<InternetDetectorViewModel>()
    val latestBooksVM = hiltNavGraphViewModel<LatestBooksVM>(navBackStackEntry)
    val scope = rememberCoroutineScope()
    val list by stateWhenStarted(flow = latestBooksVM.booksData, initial = RetrofitResult.Loading)
    list.handle(
        loading = {
            ShowLoading()
        },
        emptyData = {
            ErrorWithRetry(R.string.no_books_loaded) {
                latestBooksVM.refresh()
            }
        },
        callError = { throwable ->
            if (throwable is NoConnectionException) {
                retryOnConnectedToInternet(
                    internetDetectorVM.internetConnection,
                    scope
                ) {
                    latestBooksVM.refresh()
                }
                ErrorMessage(R.string.no_books_loaded_no_connect)
            } else {
                ErrorWithRetry(R.string.no_books_loaded) {
                    latestBooksVM.refresh()
                }
            }
        },
        apiError = { _, _ ->
            ErrorWithRetry(R.string.no_latest_books) {
                latestBooksVM.refresh()
            }
        },
        success = {
            ShowBooks(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp),
                list = this
            ) { item ->
                val bookID = item.id?.toInt() ?: return@ShowBooks
                onBookClicked(bookID, Mirrors(item.mirrors?.toList() ?: emptyList()))
            }
        }
    )
}


@Composable
fun ShowBooks(
    modifier: Modifier = Modifier,
    list: List<Book>,
    onBookClicked: (Book) -> Unit,
) {
    val scrollState = rememberLazyListState()
    scrollState.debug { "FIRST VISIBLE index ${scrollState.firstVisibleItemIndex}" }
    LazyColumn(
        state = scrollState,
        modifier = modifier,
        contentPadding = LocalWindowInsets.current.systemBars.toPaddingValues()
    ) {
        items(list, key = { it.id.toString() }) { item ->
            Book(item) {
                onBookClicked(item)
            }
        }
    }

}

@Composable
fun ShowBooksSearch(
    list: List<Book>,
    onBookClicked: (Book) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(bottom = true, left = false, right = false)
    ) {
        items(list, key = { it.id.toString() }) { item ->
            Book(item) {
                onBookClicked(item)
            }
        }
    }

}

@Composable
@Preview(showSystemUi = true, showBackground = true)
fun ShowLoading() {
    CardListShimmer()
}

