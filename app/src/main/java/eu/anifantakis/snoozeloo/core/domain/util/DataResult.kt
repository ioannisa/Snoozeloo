package eu.anifantakis.snoozeloo.core.domain.util

sealed interface DataResult<out D, out E: Error> {
    data class Success<out D>(val data: D) : DataResult<D, Nothing>
    data class Failure<out E: Error>(val error: E) : DataResult<Nothing, E>

}

inline fun <T, E: Error, R> DataResult<T, E>.map(map: (T) -> R): DataResult<R, E> {
    return when(this) {
        is DataResult.Success -> DataResult.Success(map(data))
        is DataResult.Failure -> DataResult.Failure(error)
    }
}

typealias EmptyDataResult<E> = DataResult<Unit, E>

fun <T, E: Error> DataResult<T, E>.asEmptyDataResult(): EmptyDataResult<E> {
    return map { }
}