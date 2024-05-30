package da.maximov.myTickTackToeApp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import da.maximov.myTickTackToeApp.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {

    data class CellGameFiled(val row: Int, val column: Int)

    data class StatusInfo(val status: Boolean, val side: String)

    companion object {
        const val PREF_TIME = "pref_time"
        const val PREF_GAME_FIELD = "pref_game_field"

        const val STATUS_PLAYER_WIN = 1
        const val STATUS_PLAYER_LOSE = 2
        const val STATUS_PLAYER_DRAW = 3

        const val SEARCH_DEPTH_IN_MID = 4

        const val REQUEST_POPUP_MENU = 1234

        val scores = hashMapOf(
            Pair(STATUS_PLAYER_WIN, -1.0),
            Pair(STATUS_PLAYER_LOSE, 1.0),
            Pair(STATUS_PLAYER_DRAW, 0.0)
        )
    }

    private lateinit var binding: ActivityGameBinding

    private lateinit var gameField: Array<Array<String>>

    private lateinit var settingsInfo: SettingsActivity.SettingsInfo

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)

        binding.ivToPopupMenu.setOnClickListener { showPopupMenu() }

        binding.ivToGameClose.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        with(binding) {
            ivTopLeftCell.setOnClickListener { makeStepOfUser(0, 0) } //1 //11

            ivTopCenterCell.setOnClickListener { makeStepOfUser(0, 1) } //2 //12

            ivTopRightCell.setOnClickListener { makeStepOfUser(0, 2) } //3 //13

            ivCenterLeftCell.setOnClickListener { makeStepOfUser(1, 0) } //4 //21

            ivCenterCenterCell.setOnClickListener { makeStepOfUser(1, 1) } //5 //22

            ivCenterRightCell.setOnClickListener { makeStepOfUser(1, 2) } //6 //23

            ivBottomLeftCell.setOnClickListener { makeStepOfUser(2, 0) } //7 //31

            ivBottomCenterCell.setOnClickListener { makeStepOfUser(2, 1) } //8 //32

            ivBottomRightCell.setOnClickListener { makeStepOfUser(2, 2) } //9 //33
        }


        enableEdgeToEdge()
        setContentView(binding.root)


        val time = intent.getLongExtra(MainActivity.EXTRA_TIME, 0)
        val gameField = intent.getStringExtra(MainActivity.EXTRA_GAME_FIELD)

        if (gameField != null && time != 0L && gameField != "") {
            restartGame(time, gameField)
        } else {
            initGameField()
        }


        settingsInfo = getSettingsInfo()

        mediaPlayer = MediaPlayer.create(this, R.raw.sinister_cinematic_trailer)
        mediaPlayer.isLooping = true
        setVolumeMediaPlayer(settingsInfo.soundValue)

        mediaPlayer.start()
        binding.chronometer.start()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        setVolumeMediaPlayer(settingsInfo.soundValue)
        mediaPlayer.pause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
    }

    override fun onResume() {
        super.onResume()
        setVolumeMediaPlayer(settingsInfo.soundValue)
        mediaPlayer.start()
    }

    override fun onRestart() {
        super.onRestart()
        setVolumeMediaPlayer(settingsInfo.soundValue)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_POPUP_MENU) {
            if (resultCode == RESULT_OK) {
                settingsInfo = getSettingsInfo()
                setVolumeMediaPlayer(settingsInfo.soundValue)

                mediaPlayer = MediaPlayer.create(this, R.raw.sinister_cinematic_trailer)
                mediaPlayer.isLooping = true
                setVolumeMediaPlayer(settingsInfo.soundValue)

                mediaPlayer.start()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setVolumeMediaPlayer(soundValue: Int) {
        val volume = soundValue / 100.0

        mediaPlayer.setVolume(volume.toFloat(), volume.toFloat())
    }

    private fun initGameField() {
        gameField = Array(3) { Array(3) { " " } }
    }

    private fun makeStep(row: Int, column: Int, symbol: String) {
        gameField[row][column] = symbol

        makeStepUI("$row$column", symbol)
    }

    private fun makeStepUI(position: String, symbol: String) {
        val resID = when (symbol) {
            "X" -> R.drawable.ic_cross
            "0" -> R.drawable.ic_zero
            else -> return
        }

        when (position) {
            "00" -> binding.ivTopLeftCell.setImageResource(resID)

            "01" -> binding.ivTopCenterCell.setImageResource(resID)

            "02" -> binding.ivTopRightCell.setImageResource(resID)

            "10" -> binding.ivCenterLeftCell.setImageResource(resID)

            "11" -> binding.ivCenterCenterCell.setImageResource(resID)

            "12" -> binding.ivCenterRightCell.setImageResource(resID)

            "20" -> binding.ivBottomLeftCell.setImageResource(resID)

            "21" -> binding.ivBottomCenterCell.setImageResource(resID)

            "22" -> binding.ivBottomRightCell.setImageResource(resID)
        }
    }

    private fun makeStepOfUser(row: Int, column: Int) {
        if (isEmptyField(row, column)) {
            makeStep(row, column, "X")

            val status = checkGameField(row, column, "X")

            if (status.status) {
                showGameStatus(STATUS_PLAYER_WIN)
                return
            }

            if (!isFilledGameField()) {
                val resultCell = makeStepOfAI()

                val statusAI = checkGameField(resultCell.row, resultCell.column, "0")
                if (statusAI.status) {
                    showGameStatus(STATUS_PLAYER_LOSE)
                    return
                }


                if (isFilledGameField()) {
                    showGameStatus(STATUS_PLAYER_DRAW)
                    return
                }
            } else {
                showGameStatus(STATUS_PLAYER_DRAW)
                return
            }

        } else {
            Toast.makeText(this, "Поле уже заполнено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isEmptyField(row: Int, column: Int): Boolean {
        return gameField[row][column] == " "
    }

    private fun makeStepOfAI(): CellGameFiled {
        return when (settingsInfo.lvl) {
            0 -> makeStepOfAIEasyLvl()
            1 -> makeStepOfAIMediumLvl()
            2 -> makeStepOfAIHardLvl()

            else -> CellGameFiled(0, 0)
        }
    }

    private fun makeStepOfAIEasyLvl(): CellGameFiled {
        var randRow: Int
        var randColumn: Int

        do {
            randRow = (0..2).random()
            randColumn = (0..2).random()
        } while (!isEmptyField(randRow, randColumn))

        makeStep(randRow, randColumn, "0")

        return CellGameFiled(randRow, randColumn)
    }

    private fun makeStepOfAIMediumLvl(depth: Int = SEARCH_DEPTH_IN_MID): CellGameFiled {

        var bestScore = Double.NEGATIVE_INFINITY
        var move = CellGameFiled(0, 0)

        val board = gameField.map { it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, cols ->
            cols.forEachIndexed { indexCols, _ ->
                if (board[indexRow][indexCols] == " ") {
                    board[indexRow][indexCols] = "0"
                    val score = minimax(board, false, depth)
                    board[indexRow][indexCols] = " "

                    if (score > bestScore) {
                        bestScore = score
                        move = CellGameFiled(indexRow, indexCols)
                    }
                }
            }
        }

        makeStep(move.row, move.column, "0")

        return move
    }

    private fun makeStepOfAIHardLvl(): CellGameFiled {
        var bestScore = Double.NEGATIVE_INFINITY
        var move = CellGameFiled(0, 0)

        val board = gameField.map { it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, cols ->
            cols.forEachIndexed { indexCols, _ ->
                if (board[indexRow][indexCols] == " ") {
                    board[indexRow][indexCols] = "0"
                    val score = minimax(board, false)
                    board[indexRow][indexCols] = " "

                    if (score > bestScore) {
                        bestScore = score
                        move = CellGameFiled(indexRow, indexCols)
                    }
                }
            }
        }

        makeStep(move.row, move.column, "0")

        return move
    }


    private fun minimax(board: Array<Array<String>>, isMaximizing: Boolean): Double {
        val result = checkWinner(board)
        result?.let {
            return scores[result]!!
        }

        if (isMaximizing) {
            var bestScore = Double.NEGATIVE_INFINITY

            board.forEachIndexed { indexRow, cols ->
                cols.forEachIndexed { indexCols, _ ->
                    if (board[indexRow][indexCols] == " ") {
                        board[indexRow][indexCols] = "0"
                        val score = minimax(board, false)
                        board[indexRow][indexCols] = " "

                        if (score > bestScore) {
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        } else {

            var bestScore = Double.POSITIVE_INFINITY

            board.forEachIndexed { indexRow, cols ->
                cols.forEachIndexed { indexCols, _ ->
                    if (board[indexRow][indexCols] == " ") {
                        board[indexRow][indexCols] = "X"
                        val score = minimax(board, true)
                        board[indexRow][indexCols] = " "

                        if (score < bestScore) {
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        }
    }

    private fun minimax(board: Array<Array<String>>, isMaximizing: Boolean, depth: Int): Double {

        if (depth == 0 || isGameOver(board)) {
            return evaluateBoard(board)
        }

        val result = checkWinner(board)
        result?.let {
            return scores[result]!!
        }

        if (isMaximizing) {
            var bestScore = Double.NEGATIVE_INFINITY

            board.forEachIndexed { indexRow, cols ->
                cols.forEachIndexed { indexCols, _ ->
                    if (board[indexRow][indexCols] == " ") {
                        board[indexRow][indexCols] = "0"
                        val score = minimax(board, false, depth - 1)
                        board[indexRow][indexCols] = " "

                        if (score > bestScore) {
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        } else {
            var bestScore = Double.POSITIVE_INFINITY

            board.forEachIndexed { indexRow, cols ->
                cols.forEachIndexed { indexCols, _ ->
                    if (board[indexRow][indexCols] == " ") {
                        board[indexRow][indexCols] = "X"
                        val score = minimax(board, true, depth - 1)
                        board[indexRow][indexCols] = " "

                        if (score < bestScore) {
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        }
    }

    private fun evaluateBoard(board: Array<Array<String>>): Double {
        val winner = checkWinner(board)
        return when (winner) {
            STATUS_PLAYER_LOSE -> 1.0
            STATUS_PLAYER_WIN -> -1.0
            else -> 0.0
        }
    }

    private fun isGameOver(board: Array<Array<String>>): Boolean {
        return checkWinner(board) != null || board.all { row -> row.all { it != " " } }
    }

    private fun checkWinner(board: Array<Array<String>>): Int? {
        var countRowsHu: Int
        var countRowsAI: Int
        var countLDHu = 0
        var countLDAI = 0
        var countRDHu = 0
        var countRDAI = 0

        board.forEachIndexed { indexRow, cols ->
            if (cols.all { it == "X" }) {
                return STATUS_PLAYER_WIN
            } else if (cols.all { it == "0" }) {
                return STATUS_PLAYER_LOSE
            }

            countRowsHu = 0
            countRowsAI = 0

            cols.forEachIndexed { indexCols, _ ->

                if (board[indexCols][indexRow] == "X")
                    countRowsHu++
                else if (board[indexCols][indexRow] == "0")
                    countRowsAI++

                if (indexRow == indexCols && board[indexRow][indexCols] == "X")
                    countLDHu++
                else if (indexRow == indexCols && board[indexRow][indexCols] == "0")
                    countLDAI++

                if (indexRow == 2 - indexCols && board[indexRow][indexCols] == "X")
                    countRDHu++
                else if (indexRow == 2 - indexCols && board[indexRow][indexCols] == "0")
                    countRDAI++

            }

            if (countRowsHu == 3 || countLDHu == 3 || countRDHu == 3)
                return STATUS_PLAYER_WIN
            else if (countRowsAI == 3 || countLDAI == 3 || countRDAI == 3)
                return STATUS_PLAYER_LOSE
        }

        board.forEach { it ->
            if (it.find { it == " " } != null)
                return null
        }

        return STATUS_PLAYER_DRAW
    }

    private fun checkGameField(x: Int, y: Int, symbol: String): StatusInfo {

        var row = 0
        var column = 0
        var leftDiagonal = 0
        var rightDiagonal = 0
        val gameFieldSize = gameField.size

        for (i in 0..2) {
            if (gameField[x][i] == symbol) column++
            if (gameField[i][y] == symbol) row++
            if (gameField[i][i] == symbol) leftDiagonal++
            if (gameField[i][gameFieldSize - i - 1] == symbol) rightDiagonal++
        }

        return when (settingsInfo.rules) {
            1 -> {
                if (column == gameFieldSize)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }

            2 -> {
                if (row == gameFieldSize)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }

            3 -> {
                if (
                    column == gameFieldSize
                    ||
                    row == gameFieldSize
                )
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }

            4 -> {
                if (
                    leftDiagonal == gameFieldSize
                    ||
                    rightDiagonal == gameFieldSize
                )
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }

            5 -> {
                if (
                    column == gameFieldSize
                    ||
                    leftDiagonal == gameFieldSize
                    ||
                    rightDiagonal == gameFieldSize
                )
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }

            6 -> {
                if (
                    row == gameFieldSize
                    ||
                    leftDiagonal == gameFieldSize
                    ||
                    rightDiagonal == gameFieldSize
                )
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }

            7 -> {
                if (
                    column == gameFieldSize
                    ||
                    row == gameFieldSize
                    ||
                    leftDiagonal == gameFieldSize
                    ||
                    rightDiagonal == gameFieldSize
                )
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }

            else -> StatusInfo(false, "")
        }
    }

    private fun showGameStatus(status: Int) {
        val dialog = Dialog(this, R.style.Theme_MyTickTackToeApp)
        with(dialog) {
            window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
            setContentView(R.layout.dialod_popup_status_game)
            setCancelable(true)
        }

        val image = dialog.findViewById<ImageView>(R.id.dialog_image)
        val text = dialog.findViewById<TextView>(R.id.dialog_text)
        val buttonOk = dialog.findViewById<TextView>(R.id.dialog_ok)

        buttonOk.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        when (status) {
            STATUS_PLAYER_WIN -> {
                image.setImageResource(R.drawable.ic_win)
                text.text = getString(R.string.player_win)
            }

            STATUS_PLAYER_LOSE -> {
                image.setImageResource(R.drawable.ic_lose)
                text.text = getString(R.string.player_lose)
            }

            STATUS_PLAYER_DRAW -> {
                image.setImageResource(R.drawable.ic_draw)
                text.text = getString(R.string.player_draw)
            }
        }

        dialog.show()
        binding.chronometer.stop()
    }

    private fun showPopupMenu() {
        val dialog = Dialog(this, R.style.Theme_MyTickTackToeApp)
        with(dialog) {
            window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
            setContentView(R.layout.dialog_popup_menu)
            setCancelable(true)
        }

        binding.chronometer.stop()

        val toContinue = dialog.findViewById<TextView>(R.id.dialog_continue)
        val toSettings = dialog.findViewById<TextView>(R.id.dialog_settings)
        val toExit = dialog.findViewById<TextView>(R.id.dialog_exit)

        toContinue.setOnClickListener {
            dialog.hide()
            binding.chronometer.start()
        }

        toSettings.setOnClickListener {

            dialog.hide()
            val intent = Intent(this, SettingsActivity::class.java)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_POPUP_MENU)

            binding.chronometer.start()

            settingsInfo = getSettingsInfo()
            setVolumeMediaPlayer(settingsInfo.soundValue)
        }

        toExit.setOnClickListener {
            val elapsedTime = SystemClock.elapsedRealtime() - binding.chronometer.base
            val gameField = convertGameFieldToString(gameField)

            saveGame(elapsedTime, gameField)
            dialog.dismiss()
            onBackPressedDispatcher.onBackPressed()
        }

        dialog.show()
    }

    private fun isFilledGameField(): Boolean {
        gameField.forEach { str ->
            if (str.find { it == " " } != null) return false
        }
        return true
    }

    private fun convertGameFieldToString(gameField: Array<Array<String>>): String {
        val tempArray = arrayListOf<String>()
        gameField.forEach { tempArray.add(it.joinToString(";")) }
        return tempArray.joinToString("\n")
    }

    private fun saveGame(time: Long, gameField: String) {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()) {
            putLong(PREF_TIME, time)
            putString(PREF_GAME_FIELD, gameField)
            apply()
        }
    }

    private fun restartGame(time: Long, gameField: String) {
        binding.chronometer.base = SystemClock.elapsedRealtime() - time

        this.gameField = arrayOf()

        val rows = gameField.split("\n")
        rows.forEach {
            val columns = it.split(";")
            this.gameField += columns.toTypedArray()
        }

        this.gameField.forEachIndexed { indexRow, strings ->
            strings.forEachIndexed { indexColumn, _ ->
                makeStep(indexRow, indexColumn, this.gameField[indexRow][indexColumn])
            }
        }
    }

    private fun getSettingsInfo(): SettingsActivity.SettingsInfo {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)) {

            val soundValue = getInt(SettingsActivity.PREF_SOUND_VALUE, 50)
            val lvl = getInt(SettingsActivity.PREF_LVL, 0)
            val rules = getInt(SettingsActivity.PREF_RULES, 7)

            return SettingsActivity.SettingsInfo(soundValue, lvl, rules)
        }
    }
}