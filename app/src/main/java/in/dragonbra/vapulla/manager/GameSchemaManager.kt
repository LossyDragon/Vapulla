package `in`.dragonbra.vapulla.manager

import android.text.format.DateUtils
import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.data.entity.GameSchema
import `in`.dragonbra.vapulla.retrofit.StoreFront

class GameSchemaManager(
    private val gameSchemaDao: GameSchemaDao,
    private val storeFront: StoreFront
) {

    companion object {
        const val UPDATE_INTERVAL = DateUtils.WEEK_IN_MILLIS
    }

    private val fetchingIds: MutableSet<Int> = mutableSetOf()

    fun touch(id: Int) {
        if (id <= 0) {
            return
        }

        if (fetchingIds.contains(id)) {
            return
        }

        val schema = gameSchemaDao.find(id)

        if (schema == null || schema.modifyDate < System.currentTimeMillis() - UPDATE_INTERVAL) {
            fetchingIds.add(id)

            val resp = storeFront.getAppDetails(id).execute()

            if (resp.isSuccessful) {
                val name = resp.body()?.get(id)?.data?.name

                if (name != null) {
                    gameSchemaDao.insert(GameSchema(id, name, System.currentTimeMillis()))
                }
            }

            fetchingIds.remove(id)
        }
    }
}
