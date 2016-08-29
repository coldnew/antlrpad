package utils

trait CacheStorage[TKey, TValue] {
  def put(key: TKey, value: TValue): TValue
  def get(key: TKey): Option[TValue]
  def hasKey(key: TKey): Boolean
}

class InMemoryCache[TKey, TValue] extends CacheStorage[TKey, TValue] {

  var storage = Map[TKey, TValue]()

  override def put(key: TKey, value: TValue): TValue = {
    storage += (key -> value)
    value
  }

  override def get(key: TKey): Option[TValue] = storage.get(key)
  override def hasKey(key: TKey): Boolean = storage.get(key).isDefined
}

// cache(true)    by src.hashCode value { "value to be cached" }
//       enabled?    key                expression to cache
object Cached {

  class CacheBuilderBy[TKey](val key: TKey, enabledIf: => Boolean) {
    def value[TValue](body: => TValue)(implicit cacheStorage: CacheStorage[TKey, TValue]): TValue = {
      if (enabledIf) cacheStorage.get(key).getOrElse(cacheStorage.put(key, body))
      else body
    }
  }

  class CacheBuilder(enableIf: => Boolean) {
    def by[TKey](key: => TKey) = new CacheBuilderBy[TKey](key, enableIf)
  }

  def cache(enableIf: => Boolean) = new CacheBuilder(enableIf)
}

