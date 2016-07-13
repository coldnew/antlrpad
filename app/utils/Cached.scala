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

// cache by src.hashCode value { "value to be cached" }
object Cached {

  class CacheBuilderBy[TKey](val key: TKey) {
    def value[TValue](body: => TValue)(implicit cacheStorage: CacheStorage[TKey, TValue]): TValue = {
      cacheStorage.get(key).getOrElse({
        println("Oui")
        cacheStorage.put(key, body)
      })
    }
  }

  class CacheBuilder {
    def by[TKey](key: => TKey) = new CacheBuilderBy[TKey](key)
  }

  def cache = new CacheBuilder()
}

