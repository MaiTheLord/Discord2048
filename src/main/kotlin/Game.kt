import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.ThreadChannel
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import kotlin.math.pow
import kotlin.random.Random

class Game(thread: ThreadChannel) {
    private lateinit var textMessage: Message
    private lateinit var boardMessage: Message

    private val board = Board()

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
        .append("Welcome to 2048! *Actually, its 10, but lets ignore that :)*\n")
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
                Button.primary("null_0", "\u00A0"),
                Button.primary("up", "\u25B2"),
                Button.primary("null_1", "\u00A0"),
            ),
            ActionRow.of(
                Button.primary("left", "\u25C4"),
                Button.primary("down", "\u25BC"),
                Button.primary("right", "\u25BA"),
            ),
        )

        return message.build()
    }
}

class Board {
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
                for (row in 0..3) {
                    for (column in 0..3) {
                        if (tiles[row][column] != Tile.NONE) {
                            for (newRow in 0 until row) {
                                if (tiles[newRow][column] == Tile.NONE) {
                                    tiles[newRow][column] = tiles[row][column]
                                    tiles[row][column] = Tile.NONE
                                    haveMoved = true
                                    break
                                }
                            }
                        }
                    }
                }

                for (row in 0..2) {
                    for (column in 0..3) {
                        if (tiles[row][column] != Tile.NONE && tiles[row][column] == tiles[row + 1][column]) {
                            tiles[row][column] = Tile.from(tiles[row][column].value + 1)
                            tiles[row + 1][column] = Tile.NONE
                            haveMoved = true
                            addScore(tiles[row][column].value)
                        }
                    }
                }
            }
            Direction.DOWN -> {
                for (row in 3 downTo 0) {
                    for (column in 0..3) {
                        if (tiles[row][column] != Tile.NONE) {
                            for (newRow in 3 downTo row) {
                                if (tiles[newRow][column] == Tile.NONE) {
                                    tiles[newRow][column] = tiles[row][column]
                                    tiles[row][column] = Tile.NONE
                                    haveMoved = true
                                    break
                                }
                            }
                        }
                    }
                }

                for (row in 3 downTo 1) {
                    for (column in 0..3) {
                        if (tiles[row][column] != Tile.NONE && tiles[row][column] == tiles[row - 1][column]) {
                            tiles[row][column] = Tile.from(tiles[row][column].value + 1)
                            tiles[row - 1][column] = Tile.NONE
                            haveMoved = true
                            addScore(tiles[row][column].value)
                        }
                    }
                }
            }
            Direction.LEFT -> {
                for (column in 0..3) {
                    for (row in 0..3) {
                        if (tiles[row][column] != Tile.NONE) {
                            for (newColumn in 0 until column) {
                                if (tiles[row][newColumn] == Tile.NONE) {
                                    tiles[row][newColumn] = tiles[row][column]
                                    tiles[row][column] = Tile.NONE
                                    haveMoved = true
                                    break
                                }
                            }
                        }
                    }
                }

                for (column in 0..2) {
                    for (row in 0..3) {
                        if (tiles[row][column] != Tile.NONE && tiles[row][column] == tiles[row][column + 1]) {
                            tiles[row][column] = Tile.from(tiles[row][column].value + 1)
                            tiles[row][column + 1] = Tile.NONE
                            haveMoved = true
                            addScore(tiles[row][column].value)
                        }
                    }
                }
            }
            Direction.RIGHT -> {
                for (column in 3 downTo 0) {
                    for (row in 0..3) {
                        if (tiles[row][column] != Tile.NONE) {
                            for (newColumn in 3 downTo column) {
                                if (tiles[row][newColumn] == Tile.NONE) {
                                    tiles[row][newColumn] = tiles[row][column]
                                    tiles[row][column] = Tile.NONE
                                    haveMoved = true
                                    break
                                }
                            }
                        }
                    }
                }

                for (column in 3 downTo 1) {
                    for (row in 0..3) {
                        if (tiles[row][column] != Tile.NONE && tiles[row][column] == tiles[row][column - 1]) {
                            tiles[row][column] = Tile.from(tiles[row][column].value + 1)
                            tiles[row][column - 1] = Tile.NONE
                            haveMoved = true
                            addScore(tiles[row][column].value)
                        }
                    }
                }
            }
        }

        if (haveMoved) {
            incrementTurns()

            if (tiles.any { it.any { tile -> tile == Tile.TEN } }) {
                onWon()
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

                if (isStuck) onLost()
            }
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

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}
