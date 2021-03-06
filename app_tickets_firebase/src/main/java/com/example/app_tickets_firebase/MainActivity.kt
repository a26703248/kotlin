package com.example.app_tickets_firebase

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    val database = Firebase.database
    val myRef = database.getReference("ticketsstock")
    lateinit var context: Context
    lateinit var userName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        context = this
        userName = intent.getStringExtra("userName").toString()
        //修改 Title
        title = "Hi " + userName + "的雲端購票"
        // Read from the database
        myRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val children = snapshot.children
                children.forEach {
                    Log.d("MainActivity",
                        it.key.toString() + ":" + it.value.toString())
                    when(it.key.toString()) {
                        "discount" -> TicketsStock.discount = it.value.toString().toDouble()
                        "price" -> TicketsStock.price = it.value.toString().toInt()
                        "totalAmount" -> TicketsStock.totalAmount = it.value.toString().toInt()
                    }
                }
                refreshUI()
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    fun refreshUI() {
        tv_ticket_price.setText(TicketsStock.price.toString())
        tv_ticket_discount.setText((TicketsStock.discount * 10).toString())
        tv_total_amount.setText(TicketsStock.totalAmount.toString())
    }

    // 清除欄位資料(按下清除鈕)
    fun reset(view: View) {
        refreshUI()
        et_all_tickets.setText("0")
        et_round_trip.setText("0")
    }

    // 驗證購票欄位
    fun confirmTickets(): Boolean {
        // 1. 檢查各欄位的資料是否有值, 若沒有值則自動補 0
        if(et_all_tickets.text == null || et_all_tickets.text.toString().equals("")) {
            et_all_tickets.setText("0")
        }
        if(et_round_trip.text == null || et_round_trip.text.toString().equals("")) {
            et_round_trip.setText("0")
        }

        val allTickets = et_all_tickets.text.toString().toInt()
        val roundTrip  = et_round_trip.text.toString().toInt()

        // 2. 購買票數 > 0
        if(allTickets <= 0) {
            tv_warning.setText("購買票數必須 > 0")
            Toast.makeText(context, "購買票數必須 > 0", Toast.LENGTH_SHORT).show()
            return false
        }

        // 3. 檢查剩餘票數是否足夠 ?
        val totalAmount = tv_total_amount.text.toString().toInt()
        if (allTickets > totalAmount) {
            tv_warning.setText("剩餘票數不足 !")
            Toast.makeText(context, "剩餘票數不足 !", Toast.LENGTH_SHORT).show()
            return false
        }
        // 4. 來回票組數是否設定正確 ?
        if(allTickets < roundTrip * 2) {
            tv_warning.setText("來回票組數過多 !")
            Toast.makeText(context, "剩餘票數不足 !", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // 購票流程(按下購買結帳鈕)
    fun buyTicket(view: View) {
        // 檢驗票數資訊
        if(!confirmTickets()) {
            return
        }

        val allTickets = et_all_tickets.text.toString().toInt()
        val roundTrip  = et_round_trip.text.toString().toInt()

        // 進行票務處理
        val tickets = Tickets(userName, allTickets, roundTrip)
        //  result = 總票數：%d\n來回票：%d\n單程票：%d\n總金額：$%,d
        var result = "總票數：%d\n來回票：%d\n單程票：%d\n總金額：$%,d"
        result = String.format(result,
            tickets.allTickets,
            tickets.roundTrip,
            tickets.oneWay,
            tickets.total())
        tv_result.setText(result)

        // 通知 firebase 變更 totalAmount 剩餘張數 ------------------
        val amount = TicketsStock.totalAmount - tickets.allTickets
        myRef.child("totalAmount").setValue(amount)

        //通知 firebase 紀錄訂單資料---------------------------------
        val sdf = SimpleDateFormat("yyyy-MM-dd HH-mm-ss")
        val orderTimeString = sdf.format(Date())
        val path = "order/" + userName + "/" + orderTimeString + "/"
        myRef.child(path + "allTickets").setValue(tickets.allTickets)
        myRef.child(path + "roundTrip").setValue(tickets.roundTrip)
        myRef.child(path + "oneWay").setValue(tickets.oneWay)
        myRef.child(path + "total").setValue(tickets.total())

        // 購買成功訊息
        tv_warning.setText("購買成功 !")
        Toast.makeText(context, "購買成功 !", Toast.LENGTH_SHORT).show()
    }

    //檢視購票紀錄
    fun recordTicket(view:View){
        val intent = Intent(context, OrderListActivity::class.java)
        // 設定 userName 參數資料給指定頁 (EX : OrderListActivity)
        intent.putExtra("userName", userName)
        startActivity(intent)
    }
}