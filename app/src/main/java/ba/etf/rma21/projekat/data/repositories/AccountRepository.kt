package ba.etf.rma21.projekat.data.repositories

import android.content.Context
import ba.etf.rma21.projekat.data.models.Account
import ba.etf.rma21.projekat.data.AppDatabase
import ba.etf.rma21.projekat.data.models.GrupaKviz
import ba.etf.rma21.projekat.data.models.Odgovor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception


class AccountRepository {

    companion object {
        private lateinit var context: Context

        fun setContext(_context: Context){
            context=_context
        }
        var acHash: String = "29465788-eff2-4984-93b4-f205077b0b09"

        suspend fun postaviHash(ACHash: String): Boolean {
            return withContext(Dispatchers.IO){
                try {
                    val lastUser = acHash
                    val pom = acHash
                    acHash = ACHash

                    var dataBase = AppDatabase.getInstance(context)
                    val response = ApiConfig.retrofit.dajAccount(acHash)

                    if (response.body() is Account) {
                        dataBase.accountDao().updateUser(response.body()!!.id, response.body()!!.student, response.body()!!.acHash, lastUser)
                        updatePodatke()
                        return@withContext true
                    }
                    return@withContext false
                }
                catch(error: Exception){
                    println(error.printStackTrace())
                    return@withContext false
                }
            }
        }

        fun getHash(): String {
            return acHash;
        }

        suspend fun getUser(): Account?{
            return withContext(Dispatchers.IO){
                val response = ApiConfig.retrofit.dajAccount(AccountRepository.getHash())

                when(response.body()){
                    is Account -> return@withContext response.body()
                    else -> return@withContext null
                }
            }
        }

        suspend fun updatePodatke(){
            return withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(context)
                    deleteFromDatabase()
                    val noviKvizovi = KvizRepository.getUpisane()
                    val noveGrupe = PredmetIGrupaRepository.getUpisaneGrupe()
                    val noviPredmeti = PredmetIGrupaRepository.getPredmeti().filter { predmet -> noveGrupe!!.map { novaGrupa -> novaGrupa.PredmetId }.contains(predmet.id)}
                    val noviOdgovori: MutableList<Odgovor> = mutableListOf()
                    val noviPokusaji = TakeKvizRepository.getPocetiKvizovi()

                    for(k in noviKvizovi){
                        if(noviPokusaji != null && noviPokusaji.find { pokusaj -> pokusaj.KvizId == k.id } != null) {

                            noviOdgovori.addAll(OdgovorRepository.getOdgovoriKviz(k.id))

                            for(o in noviOdgovori){
                                if(db.odgovorDao().duplikat(o.PitanjeId, k.id) == null) {
                                    println("Insertamo u bazu odgovor za pitanjeId " + o.PitanjeId + " i kviz " + k.id + "o.id = " + o.id)
                                    o.id = db.odgovorDao().maxId()?.plus(1) ?: 0

                                    println("After je " + o.id)
                                    db.odgovorDao().insert(o)
                                }
                            }
                        }
                    }


                    for(noviKviz in noviKvizovi){
                        if(db.kvizDao().duplikat(noviKviz.id) == null)
                            db.kvizDao().insert(noviKviz)
                        val pitanja = PitanjeKvizRepository.getPitanja(noviKviz.id)

                        for(p in pitanja){
                            p.KvizId = noviKviz.id

                            if(db.pitanjeDao().duplikat(p.id) == null)
                                db.pitanjeDao().insert(p)

                            else{
                                p.id = db.pitanjeDao().generateId()
                                db.pitanjeDao().insert(p)
                            }
                        }

                        val grupe = PredmetIGrupaRepository.getGrupeZaKviz(noviKviz.id)

                        for(g in grupe){

                            if(db.grupaKvizDao().duplikat(g.id, noviKviz.id) == null)
                                db.grupaKvizDao().insert(GrupaKviz((if(db.grupaKvizDao().generateId() == null) 0 else db.grupaKvizDao().generateId()!!),g.id, noviKviz.id))
                        }

                    }

                    for(pr in noviPredmeti){
                        if(db.predmetDao().duplikat(pr.id) == null)
                            db.predmetDao().insert(pr)
                    }

                    for(novaGrupa in noveGrupe){
                        if(db.grupaDao().duplikat(novaGrupa.id) == null)
                            db.grupaDao().insert(novaGrupa)
                    }

                } catch (error: Exception) {
                    println("Accoun repo ucitaj podatke " + error.printStackTrace())
                }
            }
        }

        suspend fun deleteFromDatabase(){
            return withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(context)
                    db.grupaDao().deleteAll()
                    db.predmetDao().deleteAll()
                    db.pitanjeDao().deleteAll()
                    db.kvizDao().deleteAll()
                    db.kvizTakenDao().deleteAll()
                    db.odgovorDao().deleteAll()
                } catch (error: Exception) {
                    println("Account repo hLLO: "  + error.printStackTrace())
                }
            }
        }
    }

}