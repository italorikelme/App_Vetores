import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.type.ImBoolean
import imgui.type.ImInt
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale

data class Ponto3D(var x: Float, var y: Float, var z: Float, val nome: String)

fun converterParaEspacoVisual(x: Float, y: Float, z: Float, vAxis: Int, hAxis: Int): Vector3f {
    val coords = floatArrayOf(x, y, z)
    val outroH = (setOf(0, 1, 2) - setOf(vAxis, hAxis)).first()

    val res = FloatArray(3)
    res[0] = coords[hAxis]
    res[1] = coords[vAxis]
    res[2] = coords[outroH]
    return Vector3f(res[0], res[1], res[2])
}

fun formatarNumero(valor: Float, simplificar: Boolean): String {
    return if (simplificar && valor == valor.toInt().toFloat()) {
        valor.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", valor)
    }
}

fun formatarCoordenadas(x: Float, y: Float, z: Float, simplificar: Boolean): String {
    val fx = formatarNumero(x, simplificar)
    val fy = formatarNumero(y, simplificar)
    val fz = formatarNumero(z, simplificar)
    return "($fx, $fy, $fz)"
}

fun main() {
    if (!glfwInit()) throw IllegalStateException("Não foi possível inicializar o GLFW")

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

    val window = glfwCreateWindow(1280, 720, "Visualizador 3D com ImGui - Vetores no R³", 0, 0)
    if (window == 0L) throw RuntimeException("Falha ao criar a janela GLFW")

    glfwMakeContextCurrent(window)
    glfwSwapInterval(1)
    glfwShowWindow(window)

    GL.createCapabilities()
    glEnable(GL_DEPTH_TEST)

    ImGui.createContext()
    val imguiGlfw = ImGuiImplGlfw()
    val imguiGl3 = ImGuiImplGl3()
    imguiGlfw.init(window, true)
    imguiGl3.init("#version 130")

    val pontoA = Ponto3D(0.0f, 0.0f, 0.0f, "Ponto A")
    val pontoB = Ponto3D(0.0f, 0.0f, 0.0f, "Ponto B")

    val mostrarCaixas = ImBoolean(false)
    val mostrarRotulos = ImBoolean(true)
    val simplificarZeros = ImBoolean(true)
    val fundoClaro = ImBoolean(false)

    val eixoVertical = ImInt(1)
    val eixoHorizontal = ImInt(0)
    val opcoesEixos = arrayOf("X", "Y", "Z")

    val travadoVertical = ImBoolean(false)
    val travadoHorizontal = ImBoolean(false)

    var cameraAngleX = 25f
    var cameraAngleY = -45f
    var cameraPosX = 0f
    var cameraPosY = 0f
    var cameraDistance = 14f

    var isLeftDragging = false
    var isRightDragging = false
    var lastMouseX = 0.0
    var lastMouseY = 0.0

    glfwSetMouseButtonCallback(window) { _, button, action, _ ->
        if (!ImGui.getIO().wantCaptureMouse) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                isLeftDragging = (action == GLFW_PRESS)
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                isRightDragging = (action == GLFW_PRESS)
            }
        }
    }

    glfwSetCursorPosCallback(window) { _, xpos, ypos ->
        val deltaX = (xpos - lastMouseX).toFloat()
        val deltaY = (ypos - lastMouseY).toFloat()

        if (isLeftDragging) {
            cameraAngleY += deltaX * 0.5f
            cameraAngleX += deltaY * 0.5f
        } else if (isRightDragging) {
            cameraPosX -= deltaX * 0.02f
            cameraPosY += deltaY * 0.02f
        }

        lastMouseX = xpos
        lastMouseY = ypos
    }

    glfwSetScrollCallback(window) { _, _, yoffset ->
        if (!ImGui.getIO().wantCaptureMouse) {
            cameraDistance -= yoffset.toFloat() * 0.8f
            if (cameraDistance < 2f) cameraDistance = 2f
            if (cameraDistance > 50f) cameraDistance = 50f
        }
    }

    val coordsA = floatArrayOf(pontoA.x, pontoA.y, pontoA.z)
    val coordsB = floatArrayOf(pontoB.x, pontoB.y, pontoB.z)

    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents()

        imguiGlfw.newFrame()
        ImGui.newFrame()

        ImGui.begin("Painel de Vetores e Coordenadas")
        ImGui.text("Insira as coordenadas dos pontos no R³:")
        ImGui.separator()

        val formatoInput = if (simplificarZeros.get()) "%.0f" else "%.2f"

        if (ImGui.inputFloat3("Ponto A (Ciano)", coordsA, formatoInput)) {
            pontoA.x = coordsA[0]; pontoA.y = coordsA[1]; pontoA.z = coordsA[2]
        }
        if (ImGui.inputFloat3("Ponto B (Amarelo)", coordsB, formatoInput)) {
            pontoB.x = coordsB[0]; pontoB.y = coordsB[1]; pontoB.z = coordsB[2]
        }

        ImGui.separator()
        ImGui.text("Configuração de Eixos:")

        ImGui.beginDisabled(travadoVertical.get())
        if (ImGui.combo("vertical", eixoVertical, opcoesEixos)) {
            val restantes = (setOf(0, 1, 2) - setOf(eixoVertical.get())).toList()
            if (!travadoHorizontal.get() && !restantes.contains(eixoHorizontal.get())) {
                eixoHorizontal.set(restantes[0])
            }
        }
        ImGui.endDisabled()
        ImGui.sameLine()
        ImGui.checkbox("[Travar]##V", travadoVertical)

        val disponiveisH = (setOf(0, 1, 2) - setOf(eixoVertical.get())).toList()
        val opcoesH = arrayOf(opcoesEixos[disponiveisH[0]], opcoesEixos[disponiveisH[1]])

        fun indexParaGlobal(idxLocal: Int) = disponiveisH[idxLocal]
        fun globalParaIndex(g: Int) = if (disponiveisH.contains(g)) disponiveisH.indexOf(g) else 0

        if (!disponiveisH.contains(eixoHorizontal.get())) {
            eixoHorizontal.set(disponiveisH[0])
        }

        val hLocal = ImInt(globalParaIndex(eixoHorizontal.get()))
        ImGui.beginDisabled(travadoHorizontal.get())
        if (ImGui.combo("horizontal", hLocal, opcoesH)) {
            eixoHorizontal.set(indexParaGlobal(hLocal.get()))
        }
        ImGui.endDisabled()
        ImGui.sameLine()
        ImGui.checkbox("[Travar]##H", travadoHorizontal)

        ImGui.separator()
        ImGui.checkbox("Mostrar Caixas Dimensionais", mostrarCaixas)
        ImGui.checkbox("Mostrar Coordenadas nos Pontos", mostrarRotulos)
        ImGui.checkbox("Simplificar Zeros Decimais", simplificarZeros)
        ImGui.checkbox("Fundo Claro", fundoClaro)

        ImGui.separator()
        ImGui.text("Vetor Resultante (A -> B):")

        val dvX = pontoB.x - pontoA.x
        val dvY = pontoB.y - pontoA.y
        val dvZ = pontoB.z - pontoA.z

        val vetorTexto = String.format(Locale.US, "V = (%s, %s, %s)",
            formatarNumero(dvX, simplificarZeros.get()),
            formatarNumero(dvY, simplificarZeros.get()),
            formatarNumero(dvZ, simplificarZeros.get()))
        ImGui.text(vetorTexto)

        val comprimento = sqrt(dvX * dvX + dvY * dvY + dvZ * dvZ)
        val comprimentoTexto = String.format(Locale.US, "Tamanho da Linha: %.2f", comprimento)
        ImGui.text(comprimentoTexto)

        ImGui.separator()
        if (ImGui.button("Resetar Posições")) {
            coordsA[0] = 0f; coordsA[1] = 0f; coordsA[2] = 0f
            coordsB[0] = 0f; coordsB[1] = 0f; coordsB[2] = 0f
            pontoA.x = 0f; pontoA.y = 0f; pontoA.z = 0f
            pontoB.x = 0f; pontoB.y = 0f; pontoB.z = 0f
            eixoVertical.set(1)
            eixoHorizontal.set(0)
            travadoVertical.set(false)
            travadoHorizontal.set(false)
            fundoClaro.set(false)
        }

        ImGui.end()

        val pos3DA = converterParaEspacoVisual(pontoA.x, pontoA.y, pontoA.z, eixoVertical.get(), eixoHorizontal.get())
        val pos3DB = converterParaEspacoVisual(pontoB.x, pontoB.y, pontoB.z, eixoVertical.get(), eixoHorizontal.get())

        if (fundoClaro.get()) {
            glClearColor(0.92f, 0.94f, 0.96f, 1f)
        } else {
            glClearColor(0.07f, 0.08f, 0.10f, 1f)
        }

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        val projection = Matrix4f().perspective(Math.toRadians(45.0).toFloat(), 1280f / 720f, 0.1f, 100f)
        val view = Matrix4f()
            .translate(0f, 0f, -cameraDistance)
            .rotate(Math.toRadians(cameraAngleX.toDouble()).toFloat(), 1f, 0f, 0f)
            .rotate(Math.toRadians(cameraAngleY.toDouble()).toFloat(), 0f, 1f, 0f)
            .translate(cameraPosX, cameraPosY, 0f)

        glMatrixMode(GL_PROJECTION)
        glLoadMatrixf(projection.get(FloatArray(16)))

        glMatrixMode(GL_MODELVIEW)
        glLoadMatrixf(view.get(FloatArray(16)))

        val maxCoord = max(
            arrayOf(
                kotlin.math.abs(pos3DA.x), kotlin.math.abs(pos3DA.y), kotlin.math.abs(pos3DA.z),
                kotlin.math.abs(pos3DB.x), kotlin.math.abs(pos3DB.y), kotlin.math.abs(pos3DB.z)
            ).maxOrNull() ?: 6f,
            6f
        ).toInt()

        desenharGrelhaXZ(maxCoord, fundoClaro.get())
        desenharEixosDinamicos(maxCoord.toFloat(), pos3DA, pos3DB, eixoVertical.get(), eixoHorizontal.get(), opcoesEixos)

        if (mostrarCaixas.get()) {
            desenharCaixaDimensional(pos3DA, 0.05f, 0.5f, 0.8f)
            desenharCaixaDimensional(pos3DB, 0.8f, 0.7f, 0.0f)
        }

        desenharEsferaPonto(pos3DA, 0.15f, 0.05f, 0.6f, 0.85f)
        desenharEsferaPonto(pos3DB, 0.15f, 0.9f, 0.8f, 0.1f)

        // Define a cor da seta dinamicamente: Escuro/Preto no fundo claro, Vermelho vibrante no fundo escuro
        if (fundoClaro.get()) {
            desenharSetaVetor(pos3DA, pos3DB, 0.1f, 0.1f, 0.1f)
        } else {
            desenharSetaVetor(pos3DA, pos3DB, 0.9f, 0.2f, 0.2f)
        }

        desenharCaixaProjecao(pos3DB, 0.2f, 0.7f, 0.2f)
        desenharCaixaProjecao(pos3DA, 0.2f, 0.7f, 0.2f)

        if (mostrarRotulos.get()) {
            val vp = Matrix4f().mul(projection).mul(view)
            val corTexto = if (fundoClaro.get()) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
            renderizarRotuloPonto(pontoA, pos3DA, vp, 1280f, 720f, simplificarZeros.get(), corTexto)
            renderizarRotuloPonto(pontoB, pos3DB, vp, 1280f, 720f, simplificarZeros.get(), corTexto)
        }

        ImGui.render()
        imguiGl3.renderDrawData(ImGui.getDrawData())

        glfwSwapBuffers(window)
    }

    imguiGl3.dispose()
    imguiGlfw.dispose()
    ImGui.destroyContext()

    glfwDestroyWindow(window)
    glfwTerminate()
}

fun desenharEixosDinamicos(limite: Float, posA: Vector3f, posB: Vector3f, vAxis: Int, hAxis: Int, opcoes: Array<String>) {
    val tamanhoEixoPos = max(limite + 1f, 6.0f)

    val temNeg = BooleanArray(3) { i ->
        when(i) {
            0 -> posA.x < 0 || posB.x < 0
            1 -> posA.y < 0 || posB.y < 0
            else -> posA.z < 0 || posB.z < 0
        }
    }

    val tamNeg = FloatArray(3) { i ->
        val minVal = when(i) {
            0 -> minOf(posA.x, posB.x)
            1 -> minOf(posA.y, posB.y)
            else -> minOf(posA.z, posB.z)
        }
        if (temNeg[i]) max(kotlin.math.abs(minVal) + 1f, 6.0f) else 1.0f
    }

    glLineWidth(2.0f)
    glBegin(GL_LINES)

    val outroH = (setOf(0, 1, 2) - setOf(vAxis, hAxis)).first()

    for (i in 0..2) {
        val eixoLogico = when(i) {
            0 -> hAxis
            1 -> vAxis
            else -> outroH
        }

        when (eixoLogico) {
            vAxis -> glColor3f(0.1f, 0.7f, 0.1f)
            hAxis -> glColor3f(0.8f, 0.2f, 0.2f)
            else -> glColor3f(0.1f, 0.4f, 0.8f)
        }

        val negVal = if (temNeg[i]) -tamNeg[i] else 0f
        when (i) {
            0 -> { glVertex3f(negVal, 0f, 0f); glVertex3f(tamanhoEixoPos, 0f, 0f) }
            1 -> { glVertex3f(0f, negVal, 0f); glVertex3f(0f, tamanhoEixoPos, 0f) }
            2 -> { glVertex3f(0f, 0f, negVal); glVertex3f(0f, 0f, tamanhoEixoPos) }
        }
    }
    glEnd()

    desenharCaractere3D(opcoes[hAxis][0], tamanhoEixoPos + 0.4f, 0.0f, 0.0f, 0.8f, 0.2f, 0.2f, 90f, 0f, 0f)
    desenharCaractere3D(opcoes[vAxis][0], 0.0f, tamanhoEixoPos + 0.4f, 0.0f, 0.1f, 0.7f, 0.1f, 0f, 0f, 0f)
    desenharCaractere3D(opcoes[outroH][0], 0.0f, 0.0f, tamanhoEixoPos + 0.4f, 0.1f, 0.4f, 0.8f, 90f, 0f, 90f)

    val maxInt = tamanhoEixoPos.toInt()
    glLineWidth(1.2f)

    for (i in 1..maxInt) {
        val v = i.toFloat()
        glColor3f(0.8f, 0.2f, 0.2f); glBegin(GL_LINES); glVertex3f(v, 0f, -0.1f); glVertex3f(v, 0f, 0.1f); glEnd()
        desenharNumero3D(i, v, 0.01f, 0.35f, 90f, 0f, 0f, 0.8f, 0.2f, 0.2f)

        glColor3f(0.1f, 0.7f, 0.1f); glBegin(GL_LINES); glVertex3f(-0.1f, v, 0f); glVertex3f(0.1f, v, 0f); glEnd()
        desenharNumero3D(i, 0.35f, v - 0.08f, 0f, 0f, 0f, 0f, 0.1f, 0.7f, 0.1f)

        glColor3f(0.1f, 0.4f, 0.8f); glBegin(GL_LINES); glVertex3f(-0.1f, 0f, v); glVertex3f(0.1f, 0f, v); glEnd()
        desenharNumero3D(i, 0.35f, 0.01f, v, 90f, 0f, 90f, 0.1f, 0.4f, 0.8f)
    }

    for (i in 1..tamNeg[0].toInt()) {
        val v = -i.toFloat()
        glColor3f(0.8f, 0.2f, 0.2f); glBegin(GL_LINES); glVertex3f(v, 0f, -0.1f); glVertex3f(v, 0f, 0.1f); glEnd()
        desenharNumero3D(-i, v, 0.01f, 0.35f, 90f, 0f, 0f, 0.8f, 0.2f, 0.2f)
    }
    for (i in 1..tamNeg[1].toInt()) {
        val v = -i.toFloat()
        glColor3f(0.1f, 0.7f, 0.1f); glBegin(GL_LINES); glVertex3f(-0.1f, v, 0f); glVertex3f(0.1f, v, 0f); glEnd()
        desenharNumero3D(-i, 0.35f, v - 0.08f, 0f, 0f, 0f, 0f, 0.1f, 0.7f, 0.1f)
    }
    for (i in 1..tamNeg[2].toInt()) {
        val v = -i.toFloat()
        glColor3f(0.1f, 0.4f, 0.8f); glBegin(GL_LINES); glVertex3f(-0.1f, 0f, v); glVertex3f(0.1f, 0f, v); glEnd()
        desenharNumero3D(-i, 0.35f, 0.01f, v, 90f, 0f, 90f, 0.1f, 0.4f, 0.8f)
    }
}

fun desenharDigito3D(digito: Int, x: Float, y: Float, z: Float, rotX: Float, rotY: Float, rotZ: Float, r: Float, g: Float, b: Float) {
    glColor3f(r, g, b)
    glLineWidth(2.0f)
    glPushMatrix()
    glTranslatef(x, y, z)
    glRotatef(rotX, 1f, 0f, 0f)
    glRotatef(rotY, 0f, 1f, 0f)
    glRotatef(rotZ, 0f, 0f, 1f)
    val w = 0.09f
    val h = 0.14f

    glBegin(GL_LINES)
    when (digito) {
        0 -> { glVertex3f(-w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, -h, 0f) }
        1 -> { glVertex3f(0f, -h, 0f); glVertex3f(0f, h, 0f); glVertex3f(-w * 0.6f, h * 0.6f, 0f); glVertex3f(0f, h, 0f) }
        2 -> { glVertex3f(-w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(w, 0f, 0f); glVertex3f(w, 0f, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(-w, -h, 0f); glVertex3f(-w, -h, 0f); glVertex3f(w, -h, 0f) }
        3 -> { glVertex3f(-w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(w, -h, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(w, 0f, 0f); glVertex3f(w, -h, 0f); glVertex3f(-w, -h, 0f) }
        4 -> { glVertex3f(-w, h, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(w, 0f, 0f); glVertex3f(w, h, 0f); glVertex3f(w, -h, 0f) }
        5 -> { glVertex3f(w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(w, 0f, 0f); glVertex3f(w, 0f, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(-w, -h, 0f) }
        6 -> { glVertex3f(w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, -h, 0f); glVertex3f(-w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, 0f, 0f); glVertex3f(w, 0f, 0f); glVertex3f(-w, 0f, 0f) }
        7 -> { glVertex3f(-w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(w * 0.3f, -h, 0f) }
        8 -> { glVertex3f(-w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, -h, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(w, 0f, 0f) }
        9 -> { glVertex3f(-w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, -h, 0f); glVertex3f(w, h, 0f); glVertex3f(w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, h, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(-w, 0f, 0f); glVertex3f(w, 0f, 0f) }
    }
    glEnd()
    glPopMatrix()
}

fun desenharNumero3D(
    numero: Int,
    x: Float,
    y: Float,
    z: Float,
    rotX: Float,
    rotY: Float,
    rotZ: Float,
    r: Float = 1.0f,
    g: Float = 1.0f,
    b: Float = 1.0f
) {
    val absNum = kotlin.math.abs(numero)
    val str = absNum.toString()
    var offsetH = 0f

    if (numero < 0) {
        val px = if (rotZ != 0f) x else x + offsetH
        val pz = if (rotZ != 0f) z + offsetH else z
        desenharCaractere3D('-', px, y, pz, r, g, b, rotX, rotY, rotZ)
        offsetH += 0.22f
    }

    for (char in str) {
        val digito = char.toString().toInt()
        val px = if (rotZ != 0f) x else x + offsetH
        val pz = if (rotZ != 0f) z + offsetH else z
        desenharDigito3D(digito, px, y, pz, rotX, rotY, rotZ, r, g, b)
        offsetH += 0.25f
    }
}

fun desenharCaractere3D(char: Char, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, rotX: Float, rotY: Float, rotZ: Float) {
    glColor3f(r, g, b)
    glLineWidth(2.0f)
    glPushMatrix()
    glTranslatef(x, y, z)
    glRotatef(rotX, 1f, 0f, 0f)
    glRotatef(rotY, 0f, 1f, 0f)
    glRotatef(rotZ, 0f, 0f, 1f)
    val s = 0.2f

    glBegin(GL_LINES)
    when (char) {
        'X' -> { glVertex3f(-s, -s, 0f); glVertex3f(s, s, 0f); glVertex3f(-s, s, 0f); glVertex3f(s, -s, 0f) }
        'Y' -> { glVertex3f(-s, s, 0f); glVertex3f(0f, 0f, 0f); glVertex3f(s, s, 0f); glVertex3f(0f, 0f, 0f); glVertex3f(0f, 0f, 0f); glVertex3f(0f, -s, 0f) }
        'Z' -> { glVertex3f(-s, s, 0f); glVertex3f(s, s, 0f); glVertex3f(s, s, 0f); glVertex3f(-s, -s, 0f); glVertex3f(-s, -s, 0f); glVertex3f(s, -s, 0f) }
        '-' -> { glVertex3f(-0.07f, 0f, 0f); glVertex3f(0.07f, 0f, 0f) }
    }
    glEnd()
    glPopMatrix()
}

fun desenharCaixaDimensional(pos: Vector3f, r: Float, g: Float, b: Float) {
    glColor3f(r, g, b)
    glLineWidth(1.5f)
    glEnable(GL_LINE_STIPPLE)
    glLineStipple(1, 0x0F0F.toShort())

    val x = pos.x
    val y = pos.y
    val z = pos.z

    glBegin(GL_LINES)
    glVertex3f(0f, 0f, 0f); glVertex3f(x, 0f, 0f)
    glVertex3f(0f, 0f, 0f); glVertex3f(0f, 0f, z)
    glVertex3f(x, 0f, 0f); glVertex3f(x, 0f, z)
    glVertex3f(0f, 0f, z); glVertex3f(x, 0f, z)

    glVertex3f(0f, 0f, 0f); glVertex3f(0f, y, 0f)
    glVertex3f(x, 0f, 0f); glVertex3f(x, y, 0f)
    glVertex3f(0f, 0f, z); glVertex3f(0f, y, z)
    glVertex3f(x, 0f, z); glVertex3f(x, y, z)

    glVertex3f(0f, y, 0f); glVertex3f(x, y, 0f)
    glVertex3f(0f, y, 0f); glVertex3f(0f, y, z)
    glVertex3f(x, y, 0f); glVertex3f(x, y, z)
    glVertex3f(0f, y, z); glVertex3f(x, y, z)
    glEnd()
    glDisable(GL_LINE_STIPPLE)
}

fun desenharGrelhaXZ(limite: Int, claro: Boolean) {
    if (claro) {
        glColor4f(0.75f, 0.78f, 0.82f, 0.6f)
    } else {
        glColor4f(0.18f, 0.19f, 0.22f, 0.5f)
    }
    glLineWidth(1f)
    glBegin(GL_LINES)
    for (i in -limite..limite) {
        glVertex3f(i.toFloat(), 0f, -limite.toFloat()); glVertex3f(i.toFloat(), 0f, limite.toFloat())
        glVertex3f(-limite.toFloat(), 0f, i.toFloat()); glVertex3f(limite.toFloat(), 0f, i.toFloat())
    }
    glEnd()
}

fun desenharEsferaPonto(pos: Vector3f, raio: Float, r: Float, g: Float, b: Float) {
    glColor3f(r, g, b)
    glPushMatrix()
    glTranslatef(pos.x, pos.y, pos.z)
    val lats = 16
    val lons = 16
    for (i in 0..lats) {
        val lat0 = Math.PI * (-0.5 + (i - 1).toDouble() / lats)
        val z0 = raio * sin(lat0)
        val zr0 = raio * cos(lat0)
        val lat1 = Math.PI * (-0.5 + i.toDouble() / lats)
        val z1 = raio * sin(lat1)
        val zr1 = raio * cos(lat1)
        glBegin(GL_QUAD_STRIP)
        for (j in 0..lons) {
            val lng = 2 * Math.PI * (j - 1).toDouble() / lons
            val x = cos(lng)
            val y = sin(lng)
            glVertex3d(x * zr0, y * zr0, z0)
            glVertex3d(x * zr1, y * zr1, z1)
        }
        glEnd()
    }
    glPopMatrix()
}

fun desenharSetaVetor(inicio: Vector3f, fim: Vector3f, r: Float, g: Float, b: Float) {
    val direcao = Vector3f(fim).sub(inicio)
    val comprimento = direcao.length()

    if (comprimento < 0.001f) return

    direcao.normalize()

    val raioEsfera = 0.15f
    val fimAjustado = if (comprimento > raioEsfera) {
        Vector3f(fim).sub(Vector3f(direcao).mul(raioEsfera))
    } else {
        Vector3f(fim)
    }

    val tamanhoPonta = 0.3f
    val raioPonta = 0.10f
    val pontoBasePonta = Vector3f(fimAjustado).sub(Vector3f(direcao).mul(tamanhoPonta))

    glDisable(GL_DEPTH_TEST)

    glColor3f(r, g, b)
    glLineWidth(5.0f)
    glBegin(GL_LINES)
    glVertex3f(inicio.x, inicio.y, inicio.z)
    glVertex3f(pontoBasePonta.x, pontoBasePonta.y, pontoBasePonta.z)
    glEnd()

    val perp1 = Vector3f()
    if (kotlin.math.abs(direcao.y) > 0.99f) {
        perp1.set(1f, 0f, 0f)
    } else {
        perp1.set(-direcao.z, 0f, direcao.x).normalize()
    }
    val perp2 = Vector3f(direcao).cross(perp1).normalize()

    val segmentos = 12
    glBegin(GL_TRIANGLE_FAN)
    glVertex3f(fimAjustado.x, fimAjustado.y, fimAjustado.z)
    for (i in 0..segmentos) {
        val angulo = (i.toDouble() / segmentos) * 2.0 * Math.PI
        val cosA = cos(angulo).toFloat() * raioPonta
        val sinA = sin(angulo).toFloat() * raioPonta

        val px = pontoBasePonta.x + perp1.x * cosA + perp2.x * sinA
        val py = pontoBasePonta.y + perp1.y * cosA + perp2.y * sinA
        val pz = pontoBasePonta.z + perp1.z * cosA + perp2.z * sinA
        glVertex3f(px, py, pz)
    }
    glEnd()

    glEnable(GL_DEPTH_TEST)
}

fun desenharCaixaProjecao(pos: Vector3f, r: Float, g: Float, b: Float) {
    glColor3f(r, g, b)
    glLineWidth(2f)
    glBegin(GL_LINES)
    glVertex3f(pos.x, 0f, pos.z); glVertex3f(pos.x, pos.y, pos.z)
    glEnd()
}

fun renderizarRotuloPonto(p: Ponto3D, pos3D: Vector3f, viewProjection: Matrix4f, larguraJanela: Float, alturaJanela: Float, simplificar: Boolean, corTexto: Int) {
    val clipCoords = Vector4f(pos3D.x, pos3D.y + 0.18f, pos3D.z, 1.0f).mul(viewProjection)
    if (clipCoords.w > 0.0f) {
        val invW = 1.0f / clipCoords.w
        val ndcX = clipCoords.x * invW
        val ndcY = clipCoords.y * invW

        val screenX = (larguraJanela * 0.5f * ndcX) + (larguraJanela * 0.5f)
        val screenY = (alturaJanela * 0.5f * (-ndcY)) + (alturaJanela * 0.5f)

        val coordsStr = formatarCoordenadas(p.x, p.y, p.z, simplificar)
        val textoCoord = "${p.nome}: $coordsStr"

        val drawList = ImGui.getBackgroundDrawList()
        drawList.addText(screenX - 35f, screenY - 10f, corTexto, textoCoord)
    }
}