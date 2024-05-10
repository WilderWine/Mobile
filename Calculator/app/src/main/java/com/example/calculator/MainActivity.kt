package com.example.calculator

import kotlin.math.*
import android.os.Parcel
import android.os.Parcelable
import android.app.SharedElementCallback
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.Switch
import java.math.BigDecimal

import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import android.widget.SeekBar
import java.math.RoundingMode

class ShakeDetector(val sensorManager: SensorManager,
    val flashlightToggler: FlashlightToggler) : SensorEventListener{

    private val shakeThreshold = 20
    private var lastShakeTime: Long = 0

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastShakeTime > 1000) {
            val acceleration = event.values
            val x = acceleration[0].toDouble()
            val y = acceleration[1].toDouble()
            val z = acceleration[2].toDouble()

            val accelerationMagnitude = Math.sqrt(x * x + y * y + z * z).toFloat()
            if (accelerationMagnitude > shakeThreshold) {
                lastShakeTime = currentTime
                flashlightToggler.toggle()
            }
        }
    }

    fun registerListener() {

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun unregisterListener() {
        sensorManager.unregisterListener(this)
    }

}

class FlashlightToggler(val context: Context){
    private var cameraManager: CameraManager
    private lateinit var cameraId: String
    private var isAbailable: Boolean = false
    private var isFON: Boolean = false


    init{
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try{
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: CameraAccessException){
            e.printStackTrace()
        }
    }

    fun toggle(){
        try{
            isAbailable = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            if(isAbailable ){
                cameraManager.setTorchMode(cameraId, !isOn())}
            isFON = !isFON
        }
        catch (e: CameraAccessException){
            e.printStackTrace()
        }
    }

    fun isOn(): Boolean{
        try{
            return isFON
        }
        catch (e: CameraAccessException){
            return false
        }
    }


}



class Token(var t_type: String, var t_value: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(t_type)
        parcel.writeString(t_value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Token> {
        override fun createFromParcel(parcel: Parcel): Token {
            return Token(parcel)
        }

        override fun newArray(size: Int): Array<Token?> {
            return arrayOfNulls(size)
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var switchTheme: Switch
    private lateinit var brightnessBar: SeekBar
    private  lateinit var shakeDetector: ShakeDetector
    private lateinit var flashlightToggler: FlashlightToggler


    private var tokens = ArrayList<Token>()

    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sharedPreferences: SharedPreferences
    private var current_number: String = ""
    private var isDotUsed: Boolean = false
    private var scobeBalance = 0

    private val digit_buttons: List<Int> = listOf(R.id.zero_d, R.id.one_d, R.id.two_d,
                                                    R.id.three_d, R.id.four_d, R.id.five_d,
                                                    R.id.six_d, R.id.seven_d, R.id.eight_d,
                                                    R.id.nine_d, R.id.dot_d)
    private val functions_buttons: List<Int> = listOf(R.id.sin_f, R.id.cos_f, R.id.tan_f,
        R.id.ctan_f, R.id.log_f, R.id.ln_f,
        /*R.id.percent_f,*/ R.id.exp_f, R.id.factorial_f,
        R.id.sqrt_f)

    private val scobes_buttons: List<Int> = listOf(R.id.l_scobe_f, R.id.r_scobe_f)
    private val operators_buttons: List<Int> = listOf(R.id.plus_op, R.id.multiply_op, R.id.minus_op,
        R.id.divide_op, R.id.pow_op)
    private val equal_buttons: List<Int> = listOf(R.id.equals_eq)
    private val deletes_buttons: List<Int> = listOf(R.id.clear_del, R.id.clear1_del)


    fun calculate(tokens:List<Token>): String{

        try {

            fun priority(operator: String): Int {
                if (operator == "+" || operator == "-") {
                    return 1
                } else if (operator == "x" || operator == "/") {
                    return 2
                } else if (operator == "^") {
                    return 3
                }
                return -1
            }

            fun operation(a: BigDecimal, b: BigDecimal, operator: String): BigDecimal? {
                try {
                    when (operator) {
                        "+" -> {
                            return a.add(b);
                        }

                        "-" -> {
                            return a.subtract(b);
                        }

                        "x" -> {
                            return a.multiply(b);
                        }

                        "/" -> {
                            var aa = a.divide(b, 40, RoundingMode.HALF_UP)
                            return aa
                           /* if (b.compareTo(BigDecimal.ZERO) == 0) {
                                return null
                            }
                            val aDouble = a.toDouble()
                            val bDouble = b.toDouble()
                            val res = BigDecimal(aDouble / bDouble)
                            return res*/
                        }

                        "^" -> {
                            val aDouble = a.toDouble()
                            val bDouble = b.toDouble()
                            val resDouble = Math.pow(aDouble, bDouble)
                            if (resDouble.isNaN() or resDouble.isInfinite()) return null;
                            val res = BigDecimal(resDouble)
                            return res
                        }

                        else -> {
                            return null
                        }
                    }
                    return null
                } catch (e: Exception) {
                    return null
                }
            }

            fun function(a: Double, func: String): Double? {
                fun factorial(n: Int): Long {
                    if (n == 0 || n == 1) {
                        return 1
                    } else {
                        return n * factorial(n - 1)
                    }
                }
                try {
                    when (func) {
                        "(" -> {
                            return a
                        }

                        "sin(" -> {
                            return sin(a)
                        }

                        "cos(" -> {
                            return cos(a)
                        }

                        "tan(" -> {
                            return tan(a)
                        }

                        "ctan(" -> {
                            return 1 / tan(a)
                        }

                        "log(" -> {
                            return log10(a)
                        }

                        "ln(" -> {
                            return ln(a)
                        }

                        "sqrt(" -> {
                            return sqrt(a)
                        }

                        "exp(" -> {
                            return exp(a)
                        }

                        "%(" -> {
                            return (BigDecimal(a).divide(BigDecimal("100.0")).toDouble())
                        }

                        "!(" -> {
                            val aString = a.toString()
                            val decimalPart = aString.substringAfter(".", "")
                            val allZeros = decimalPart.all { it == '0' }
                            val aInt = if (allZeros) {
                                aString.substringBefore(".").toInt()
                            } else {
                                null
                            }
                            if (aInt == null) {
                                return null
                            } else {
                                return factorial(aInt).toDouble()
                            }
                        }
                    }
                } catch (e: Exception) {
                    return null
                }
                return null
            }

            var numbersStack = ArrayDeque<BigDecimal>()
            var operatorsStack = ArrayDeque<String>()
            var res: BigDecimal = BigDecimal(0.0)

            if (tokens.isEmpty()) return "0"
            if (scobeBalance != 0) return "ERROR"
            if (tokens.last().t_type != "number" && tokens.last().t_type != "right_scobe") return "ERROR"

            if (tokens.last().t_type == "number" && tokens.last().t_value.endsWith('.'))
                tokens.last().t_value += '0'

            for (token in tokens) {
                if (token.t_type == "number") {
                    numbersStack.add(BigDecimal(token.t_value))
                } else if (token.t_value.endsWith('(')) {
                    operatorsStack.add(token.t_value)
                } else if (token.t_type == "operator") {
                    if (operatorsStack.isEmpty() || priority(operatorsStack.last()) < priority(token.t_value)) {
                        operatorsStack.add(token.t_value)
                    } else {
                        //var sub_res: Double = Double.NaN
                        while (!(operatorsStack.isEmpty() ||
                                    priority(operatorsStack.last()) < priority(token.t_value) ||
                                    operatorsStack.last().endsWith('('))
                        ) {
                            val b = numbersStack.removeLast()
                            val a = numbersStack.removeLast()
                            val operator = operatorsStack.removeLast()
                            val sub_res = operation(a, b, operator)
                            if (sub_res == null) return "ERROR"
                            numbersStack.add(sub_res)
                        }
                        operatorsStack.add(token.t_value)
                    }
                } else if (token.t_type == "right_scobe") {
                    while (!(operatorsStack.last().endsWith('('))) {
                        val b = numbersStack.removeLast()
                        val a = numbersStack.removeLast()
                        val operator = operatorsStack.removeLast()
                        val sub_res = operation(a, b, operator)
                        if (sub_res == null) return "ERROR"
                        numbersStack.add(sub_res)
                    }
                    val func = operatorsStack.removeLast()
                    val a = numbersStack.removeLast()
                    val sub_res = function(a.toDouble(), func)
                    if (sub_res == null) return "ERROR"
                    numbersStack.add(BigDecimal(sub_res))
                }
            }
            while (!operatorsStack.isEmpty()) {
                val b = numbersStack.removeLast()
                val a = numbersStack.removeLast()
                val operator = operatorsStack.removeLast()
                val sub_res = operation(a, b, operator)
                if (sub_res == null) return "ERROR"
                numbersStack.add(sub_res)
            }
            res = numbersStack.removeLast()
            if (res == null || !numbersStack.isEmpty()) return "ERROR"
            return res.toString()

        }
        catch (e: Exception){
            return "ERROR"

        }
    }

    fun print_tokens(tokens:List<Token>){
        try {
            var res: String = ""
            for (token in tokens) {
                res += token.t_value + " "
            }
            res = res.substring(0, res.length - 1)
            findViewById<android.widget.TextView>(R.id.textView).text = res
        }
        catch (e: Exception){
            findViewById<android.widget.TextView>(R.id.textView).text = "ERROR"
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList("tokens", ArrayList(tokens))
        outState.putInt("scobeBalance", scobeBalance)
        outState.putBoolean("isDotUsed", isDotUsed)

        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        if (savedInstanceState != null) {
            tokens = savedInstanceState.getParcelableArrayList<Token>("tokens")?.let { ArrayList(it) } ?: ArrayList()
            isDotUsed = savedInstanceState.getBoolean("isDotUsed")
            scobeBalance = savedInstanceState.getInt("scobeBalance")
        }


        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val nightMode:Boolean = sharedPreferences.getBoolean("nightMode", false)

        if (nightMode) {
             AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
         } else {
             AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
         }


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val horizontalScrollView = findViewById<HorizontalScrollView>(R.id.horSView)
        horizontalScrollView.post {
            horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
        }

        // flashlight

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        flashlightToggler = FlashlightToggler(this)
        shakeDetector = ShakeDetector(sensorManager, flashlightToggler)

        // brightness

        brightnessBar = findViewById(R.id.seekBar)
        fun change(newBrightness: Float){
            val params = window.attributes
            params.screenBrightness = newBrightness
            window.attributes = params
        }

        fun getBrightness(): Float{
            val contentResolver: ContentResolver = applicationContext.contentResolver
            var curBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS).toFloat()
            curBrightness = curBrightness / 255
            return curBrightness
        }

        brightnessBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val brightnessValue = progress.toFloat() / 100
                    change(brightnessValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?){}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        val currentBrightness = getBrightness()
        brightnessBar.progress = (currentBrightness * 100).toInt()


        // calculator

        if(tokens.isEmpty()){
            findViewById<android.widget.TextView>(R.id.textView).text = "0"
        }
        else{
            print_tokens(tokens)
            horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
        }

        switchTheme = findViewById(R.id.toggleButton)

        switchTheme.setOnClickListener {
            if (nightMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor = sharedPreferences.edit()

                editor.putBoolean("nightMode", false)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                editor = sharedPreferences.edit()
                editor.putBoolean("nightMode", true)
            }
            editor.apply()
            recreate()
        }

        val clickListenerDigit = View.OnClickListener { view ->
            when (view.id) {
                R.id.dot_d -> {
                    if (!isDotUsed){
                        if(tokens.size == 0){
                            tokens.add(Token("number", "0."))
                        }
                        else if(tokens.last().t_type == "number"){
                            tokens.last().t_value += "."
                            isDotUsed = true
                        }
                    }
                }
                else -> {
                    if (tokens.size == 0 || tokens.last().t_type != "number"){
                        if (tokens.size != 0 && tokens.last().t_type == "right_scobe"){
                            tokens.add(Token("operator", "x"))
                        }
                        tokens.add(Token("number", (view as Button).text.toString()))
                    }
                    else{
                        tokens.last().t_value += (view as Button).text.toString()
                    }
                }

            }
            print_tokens(tokens)
            horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
        }

        for (d in digit_buttons){
            val button: Button = findViewById(d)
            button.setOnClickListener(clickListenerDigit)
        }

        val clickListenerScobe = View.OnClickListener { view ->
            when (view.id) {
                R.id.l_scobe_f -> {
                    scobeBalance += 1
                    if (tokens.size > 0 && (tokens.last().t_type == "right_scobe" ||
                                tokens.last().t_type == "number")){
                        if (tokens.last().t_value.endsWith('.'))
                            tokens.last().t_value += "0"
                        tokens.add(Token("operator", "x"))
                    }
                    tokens.add(Token("left_scobe", "("))

                }
                R.id.r_scobe_f -> {
                    if (scobeBalance > 0 && !tokens.isEmpty() && !tokens.last().t_value.endsWith('(') &&
                        tokens.last().t_type != "operator") {
                        scobeBalance -= 1;
                        tokens.add(Token("right_scobe", ")"))
                    }
                }

            }
            if (!tokens.isEmpty()) {
                isDotUsed = false
                print_tokens(tokens)
                horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
            }
        }
        for (d in scobes_buttons){
            val button: Button = findViewById(d)
            button.setOnClickListener(clickListenerScobe)
        }

        val clickListenerOperator = View.OnClickListener { view ->

            val unary_mimus:Boolean = view.id == R.id.minus_op && (!tokens.isEmpty() &&
                    tokens.last().t_value.endsWith('(') || tokens.isEmpty())
            val ordinary_operator:Boolean = !tokens.isEmpty() && (tokens.last().t_type == "number"
                    || tokens.last().t_type == "right_scobe")
            val operator_change: Boolean = !tokens.isEmpty() && tokens.last().t_type == "operator"
            if(unary_mimus){
                tokens.add(Token("number", "-1"))
                tokens.add(Token("operator", "x"))
                print_tokens(tokens)
                horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
            }
            else if(ordinary_operator){
                if (tokens.last().t_type == "number" && tokens.last().t_value.endsWith('.')){
                    tokens.last().t_value += "0"
                }
                tokens.add(Token("operator", findViewById<Button>(view.id).text.toString()))
                print_tokens(tokens)
                horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
                isDotUsed = false
            }
            else if(operator_change){
                tokens.remove(tokens.last())
                tokens.add(Token("operator", findViewById<Button>(view.id).text.toString()))
                print_tokens(tokens)
                horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
            }
        }
        for (d in operators_buttons){
            val button: Button = findViewById(d)
            button.setOnClickListener(clickListenerOperator)
        }

        val clickListenerDeletes = View.OnClickListener { view ->
            if (findViewById<android.widget.TextView>(R.id.textView).text == "ERROR" ||
                findViewById<android.widget.TextView>(R.id.textView).text.contains("Infinity")){
                var all_not_deletes: ArrayList<Int> = ArrayList()
                all_not_deletes.addAll(equal_buttons)
                all_not_deletes.addAll(scobes_buttons)
                all_not_deletes.addAll(functions_buttons)
                all_not_deletes.addAll(operators_buttons)
                all_not_deletes.addAll(digit_buttons)
                for (button_id in all_not_deletes) {
                    var button = findViewById<Button>(button_id)
                    button.isEnabled = true
                }
                tokens.clear()
                isDotUsed = false
                scobeBalance = 0
                findViewById<android.widget.TextView>(R.id.textView).text = "0"
            }
            else{
                if(view.id == R.id.clear_del){
                    tokens.clear()
                    findViewById<android.widget.TextView>(R.id.textView).text = "0"
                    isDotUsed = false
                    scobeBalance = 0
                }
                else if(view.id == R.id.clear1_del){
                    if(tokens.size > 0){
                        if(tokens.last().t_type == "number"){
                            var number = tokens.last()
                            if(number.t_value.contains('E') || number.t_value.length == 1 ||
                                number.t_value.length == 2 && number.t_value[0] == '-'){
                                tokens.remove(tokens.last())
                            }
                            else{
                                if (number.t_value.endsWith('.')){
                                    isDotUsed = false
                                }
                                number.t_value = number.t_value.substring(0, number.t_value.length-1)
                            }
                            if(tokens.isEmpty()){
                                findViewById<android.widget.TextView>(R.id.textView).text = "0"

                            }
                           else  print_tokens(tokens)
                            horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
                        }
                        else{
                            var removed = tokens.last()
                            if (removed.t_type == "function" || removed.t_type == "left_scobe"){
                                scobeBalance -= 1
                            }
                            else if (removed.t_type == "right_scobe"){
                                scobeBalance += 1
                            }
                            tokens.remove(removed)
                            if (tokens.last().t_type == "number"){
                                isDotUsed = tokens.last().t_value.contains('.')
                            }
                            print_tokens(tokens)
                            horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
                        }
                    }
                }
            }
        }
        for (d in deletes_buttons){
            val button: Button = findViewById(d)
            button.setOnClickListener(clickListenerDeletes)
        }

        val clickListenerFunctions = View.OnClickListener { view ->

            scobeBalance += 1;
            if (tokens.size > 0 && (tokens.last().t_type == "right_scobe" ||
                        tokens.last().t_type == "number")){
                if (tokens.last().t_value.endsWith('.'))
                    tokens.last().t_value += "0"
                tokens.add(Token("operator", "x"))
            }
            tokens.add(Token("function", (view as Button).text.toString() + "("))
            print_tokens(tokens)
            isDotUsed = false
            horizontalScrollView.fullScroll(View.FOCUS_RIGHT)

        }
        for (d in functions_buttons){
            val button: Button = findViewById(d)
            button.setOnClickListener(clickListenerFunctions)
        }

       findViewById<Button>(equal_buttons[0]).setOnClickListener{
           val res: String = calculate(tokens)
           findViewById<android.widget.TextView>(R.id.textView).text = res
           isDotUsed = false
           if(res == "ERROR" || res.contains("Infinity")){
               var all_not_deletes: ArrayList<Int> = ArrayList()
               all_not_deletes.addAll(equal_buttons)
               all_not_deletes.addAll(scobes_buttons)
               all_not_deletes.addAll(functions_buttons)
               all_not_deletes.addAll(operators_buttons)
               all_not_deletes.addAll(digit_buttons)
               for (button_id in all_not_deletes){
                   var button = findViewById<Button>(button_id)
                   button.isEnabled = false
               }
               tokens.clear()
               return@setOnClickListener
           }
           tokens.clear()
           tokens.add(Token("number", res))
           if(res.contains('.') || res.contains("E")) isDotUsed = true
           scobeBalance = 0
           print_tokens(tokens)
           horizontalScrollView.fullScroll(View.FOCUS_RIGHT)
       }
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.registerListener()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.unregisterListener()
    }

}