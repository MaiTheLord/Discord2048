import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.ThreadChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.random.Random

class Game(thread: ThreadChannel, private val user: User) {
    private val board = Board()

    private lateinit var textMessage: Message
    private lateinit var boardMessage: Message

    var isOver = false
    private var score = 0
    private var turn = 1

    init {
        thread.sendMessage(createGameTextMessage()).queue { textMessage ->
            this.textMessage = textMessage
            thread.sendMessage(createGameBoardMessage()).queue { boardMessage -> this.boardMessage = boardMessage }
        }
    }

    private fun refreshMessages() {
        textMessage.editMessage(createGameTextMessage()).queue()
        boardMessage.editMessage(createGameBoardMessage()).queue()
    }

    fun move(direction: Direction) {
        if (isOver) return
        board.move(direction,  { score += 2.toDouble().pow(it + 1).toInt() }, { turn++ }, ::onWin, ::onLose)
        refreshMessages()
        thread {
            Database.InsertIntoLeaderboard.execute(user, boardMessage.guild, score, if (isOver) turn else turn - 1)
        }
    }

    private fun onWin() {
        isOver = true
        boardMessage.channel.sendMessage("**You've won!** With a score of **$score** in **$turn** turns.").queue()
    }

    private fun onLose() {
        isOver = true
        boardMessage.channel.sendMessage("**You've lost!** With a score of **$score** in **$turn** turns.").queue()
    }

    private fun createGameTextMessage() = MessageBuilder()
        .append("**<@${user.id}>, Welcome to 2048!** *Actually, its 10, but lets ignore that :)*\n")
        .append("I assume you know the rules. Just press the buttons to move the tiles.\n")
        .append("The only difference between 2048 and 10 is that the number increments instead of doubles itself, and your goal is to reach 10.\n")
        .append("\n")
        .append("Score: **$score** | Turn: **$turn**\n")
        .build()

    private fun createGameBoardMessage(): Message {
        val message = MessageBuilder()

        for (row in board.tiles) {
            for (tile in row) message.append(tile.emoji)
            message.append("\n")
        }

        message.setActionRows(
            ActionRow.of(
                Button.secondary("null", "\u00A0"),
                Button.primary("up", "\u25B2"),
                Button.danger("exit", "\u2716"),
            ),
            ActionRow.of(
                Button.primary("left", "\u25C4"),
                Button.primary("down", "\u25BC"),
                Button.primary("right", "\u25BA"),
            ),
        )

        return message.build()
    }

    private class Board {
        val tiles = Array(4) { Array(4) { Tile.NONE } }

        init {
            repeat(2) {
                addToRandomTile()
            }
        }

        fun move(direction: Direction, addScore: (Int) -> Unit, incrementTurns: () -> Unit, onWon: () -> Unit, onLost: () -> Unit) {
            var haveMoved = false

            when (direction) {
                Direction.UP -> {
                    val merged = ArrayList<Pair<Int, Int>>()

                    for (row in 0..3) {
                        for (column in 0..3) {
                            if (tiles[row][column] != Tile.NONE) {
                                var bestRow = null as Int?

                                for (newRow in row - 1 downTo 0) {
                                    if (tiles[newRow][column] == Tile.NONE || (tiles[newRow][column] == tiles[row][column] && !merged.contains(Pair(newRow, column)))) {
                                        bestRow = newRow
                                    } else {
                                        break
                                    }
                                }

                                if (bestRow != null) {
                                    if (tiles[bestRow][column] == Tile.NONE) {
                                        tiles[bestRow][column] = tiles[row][column]
                                        tiles[row][column] = Tile.NONE
                                        haveMoved = true
                                    } else {
                                        tiles[bestRow][column] = Tile.from(tiles[bestRow][column].value + 1)
                                        tiles[row][column] = Tile.NONE
                                        merged.add(Pair(bestRow, column))
                                        addScore(tiles[bestRow][column].value)
                                        haveMoved = true
                                    }
                                }
                            }
                        }
                    }
                }
                Direction.DOWN -> {
                    val merged = ArrayList<Pair<Int, Int>>()

                    for (row in 3 downTo 0) {
                        for (column in 0..3) {
                            if (tiles[row][column] != Tile.NONE) {
                                var bestRow = null as Int?

                                for (newRow in row + 1..3) {
                                    if (tiles[newRow][column] == Tile.NONE || (tiles[newRow][column] == tiles[row][column] && !merged.contains(Pair(newRow, column)))) {
                                        bestRow = newRow
                                    } else {
                                        break
                                    }
                                }

                                if (bestRow != null) {
                                    if (tiles[bestRow][column] == Tile.NONE) {
                                        tiles[bestRow][column] = tiles[row][column]
                                        tiles[row][column] = Tile.NONE
                                        haveMoved = true
                                    } else {
                                        tiles[bestRow][column] = Tile.from(tiles[bestRow][column].value + 1)
                                        tiles[row][column] = Tile.NONE
                                        merged.add(Pair(bestRow, column))
                                        addScore(tiles[bestRow][column].value)
                                        haveMoved = true
                                    }
                                }
                            }
                        }
                    }
                }
                Direction.LEFT -> {
                    val merged = ArrayList<Pair<Int, Int>>()

                    for (column in 0..3) {
                        for (row in 0..3) {
                            if (tiles[row][column] != Tile.NONE) {
                                var bestColumn = null as Int?

                                for (newColumn in column - 1 downTo 0) {
                                    if (tiles[row][newColumn] == Tile.NONE || (tiles[row][newColumn] == tiles[row][column] && !merged.contains(Pair(row, newColumn)))) {
                                        bestColumn = newColumn
                                    } else {
                                        break
                                    }
                                }

                                if (bestColumn != null) {
                                    if (tiles[row][bestColumn] == Tile.NONE) {
                                        tiles[row][bestColumn] = tiles[row][column]
                                        tiles[row][column] = Tile.NONE
                                        haveMoved = true
                                    } else {
                                        tiles[row][bestColumn] = Tile.from(tiles[row][bestColumn].value + 1)
                                        tiles[row][column] = Tile.NONE
                                        merged.add(Pair(row, bestColumn))
                                        addScore(tiles[row][bestColumn].value)
                                        haveMoved = true
                                    }
                                }
                            }
                        }
                    }
                }
                Direction.RIGHT -> {
                    val merged = ArrayList<Pair<Int, Int>>()

                    for (column in 3 downTo 0) {
                        for (row in 0..3) {
                            if (tiles[row][column] != Tile.NONE) {
                                var bestColumn = null as Int?

                                for (newColumn in column + 1..3) {
                                    if (tiles[row][newColumn] == Tile.NONE || (tiles[row][newColumn] == tiles[row][column] && !merged.contains(Pair(row, newColumn)))) {
                                        bestColumn = newColumn
                                    } else {
                                        break
                                    }
                                }

                                if (bestColumn != null) {
                                    if (tiles[row][bestColumn] == Tile.NONE) {
                                        tiles[row][bestColumn] = tiles[row][column]
                                        tiles[row][column] = Tile.NONE
                                        haveMoved = true
                                    } else {
                                        tiles[row][bestColumn] = Tile.from(tiles[row][bestColumn].value + 1)
                                        tiles[row][column] = Tile.NONE
                                        merged.add(Pair(row, bestColumn))
                                        addScore(tiles[row][bestColumn].value)
                                        haveMoved = true
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (haveMoved) {
                if (tiles.any { it.any { tile -> tile == Tile.TEN } }) {
                    onWon()
                    return
                }

                addToRandomTile()

                if (tiles.flatten().none { it == Tile.NONE}) {
                    var isStuck = true

                    for (x in 0..3) {
                        for (y in 0..3) {
                            if (x < 3) {
                                if (tiles[x][y] == tiles[x + 1][y]) {
                                    isStuck = false
                                    break
                                }
                            }

                            if (y < 3) {
                                if (tiles[x][y] == tiles[x][y + 1]) {
                                    isStuck = false
                                    break
                                }
                            }
                        }

                        if (!isStuck) break
                    }

                    if (isStuck) {
                        onLost()
                        return
                    }
                }

                incrementTurns()
            }
        }

        private fun addToRandomTile() {
            val emptyTiles = ArrayList<Pair<Int, Int>>()

            for (x in 0..3) {
                for (y in 0..3) {
                    if (tiles[x][y] == Tile.NONE) {
                        emptyTiles.add(Pair(x, y))
                    }
                }
            }

            val (x, y) = emptyTiles.random()

            tiles[x][y] = if (Random.nextInt(10) != 0) Tile.ZERO else Tile.ONE
        }

        enum class Tile(val value: Int, val emoji: String) {
            NONE(-1, ":purple_square:"),
            ZERO(0, ":zero:"),
            ONE(1, ":one:"),
            TWO(2, ":two:"),
            THREE(3, ":three:"),
            FOUR(4, ":four:"),
            FIVE(5, ":five:"),
            SIX(6, ":six:"),
            SEVEN(7, ":seven:"),
            EIGHT(8, ":eight:"),
            NINE(9, ":nine:"),
            TEN(10, ":keycap_ten:");

            companion object {
                fun from(value: Int): Tile {
                    return values().first { it.value == value }
                }
            }
        }
    }

    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
