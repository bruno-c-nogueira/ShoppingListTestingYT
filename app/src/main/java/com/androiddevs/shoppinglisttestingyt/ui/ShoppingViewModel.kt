package com.androiddevs.shoppinglisttestingyt.ui

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androiddevs.shoppinglisttestingyt.data.local.ShoppingItem
import com.androiddevs.shoppinglisttestingyt.other.Constants
import com.androiddevs.shoppinglisttestingyt.other.Event
import com.androiddevs.shoppinglisttestingyt.other.Resource
import com.androiddevs.shoppinglisttestingyt.remote.response.ImageResponse
import com.androiddevs.shoppinglisttestingyt.repository.ShoppingRepository
import kotlinx.coroutines.launch
import java.lang.Exception

class ShoppingViewModel @ViewModelInject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    val shoppingItems = repository.observeAllShoppingItems()

    val totalPrice = repository.observeTotalPrice()

    private val _images = MutableLiveData<Event<Resource<ImageResponse>>>()
    val images: LiveData<Event<Resource<ImageResponse>>> = _images

    private val _currentImageUrl = MutableLiveData<String>()
    val currentImageUrl: LiveData<String> = _currentImageUrl

    private val _insertShoppingItemStatus = MutableLiveData<Event<Resource<ShoppingItem>>>()
    val insertShoppingItemStatus: LiveData<Event<Resource<ShoppingItem>>> =
        _insertShoppingItemStatus

    fun setCurrentImageUrl(url: String) {
        _currentImageUrl.postValue(url)
    }

    fun deleteShoppingItem(shoppingItem: ShoppingItem) = viewModelScope.launch {
        repository.deleteShoppingItem(shoppingItem)
    }

    fun insertShoppingItemDB(shoppingItem: ShoppingItem) = viewModelScope.launch {
        repository.insertShoppingItem(shoppingItem)
    }

    fun insertShoppingItem(name: String, amountString: String, priceString: String) {
        if (name.isEmpty() || amountString.isEmpty() || priceString.isEmpty()) {
            _insertShoppingItemStatus.postValue(
                Event(
                    Resource.error(
                        "fields must not be empty",
                        null
                    )
                )
            )
            return
        }
        if (name.length > Constants.MAX_NAME_LENGTH) {
            _insertShoppingItemStatus.postValue(
                Event(
                    Resource.error(
                        "The name of the item" +
                                "must not exceed ${Constants.MAX_NAME_LENGTH} characters", null
                    )
                )
            )
            return
        }
        if (priceString.length > Constants.MAX_PRICE) {
            _insertShoppingItemStatus.postValue(
                Event(
                    Resource.error(
                        "The price of the item" +
                                "must not exceed ${Constants.MAX_PRICE} characters", null
                    )
                )
            )
            return
        }

        val amount = try {
            amountString.toInt()
        } catch (e: Exception) {
            _insertShoppingItemStatus.postValue(
                Event(
                    Resource.error(
                        "insert shopping item with too high amount, returns error", null
                    )
                )
            )
            return
        }

        val shoppingItem =
            ShoppingItem(name, amount, priceString.toFloat(), _currentImageUrl.value ?: "")
        insertShoppingItemDB(shoppingItem)
        searchForImage("")
        _insertShoppingItemStatus.postValue(Event(Resource.success(shoppingItem)))
    }

    fun searchForImage(imageQuery: String) {
        if (imageQuery.isEmpty()) {
            return
        }
        _images.value = Event(Resource.loading(null))
        viewModelScope.launch {
            val response = repository.searchForImage(imageQuery)
            _images.value = Event(response)
        }
    }


}