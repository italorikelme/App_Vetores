import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import imgui.type.ImBoolean
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

// Formatação auxiliar para simplificar zeros decimais desnecessários
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

    // Inicialização do Dear ImGui
    ImGui.createContext()
    val imguiGlfw = ImGuiImplGlfw()
    val imguiGl3 = ImGuiImplGl3()
    imguiGlfw.init(window, true)
    imguiGl3.init("#version 130")

    // Pontos editáveis
    val pontoA = Ponto3D(5.0f, 0.0f, 0.0f, "Ponto A")
    val pontoB = Ponto3D(1.0f, 5.0f, 2.0f, "Ponto B")

    // Variáveis booleanas do ImGui
    val mostrarCaixas = ImBoolean(false)
    val mostrarRotulos = ImBoolean(true)
    val simplificarZeros = ImBoolean(true)

    // Controlo de câmara
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

        // --- PAINEL IMGUI ---
        ImGui.begin("Painel de Vetores e Coordenadas")
        ImGui.text("Insira as coordenadas dos pontos no R³:")
        ImGui.separator()

        if (ImGui.inputFloat3("Ponto A (Ciano)", coordsA)) {
            pontoA.x = coordsA[0]
            pontoA.y = coordsA[1]
            pontoA.z = coordsA[2]
        }

        if (ImGui.inputFloat3("Ponto B (Amarelo)", coordsB)) {
            pontoB.x = coordsB[0]
            pontoB.y = coordsB[1]
            pontoB.z = coordsB[2]
        }

        ImGui.separator()
        ImGui.checkbox("Mostrar Caixas Dimensionais (X, Y, Z)", mostrarCaixas)
        ImGui.checkbox("Mostrar Coordenadas nos Pontos", mostrarRotulos)
        ImGui.checkbox("Simplificar Zeros Decimais (ex: 2 em vez de 2.0)", simplificarZeros)

        ImGui.separator()
        ImGui.text("Vetor Resultante (A -> B):")

        val vx = pontoB.x - pontoA.x
        val vy = pontoB.y - pontoA.y
        val vz = pontoB.z - pontoA.z

        val vetorTexto = String.format(Locale.US, "V = (%.2f, %.2f, %.2f)", vx, vy, vz)
        ImGui.text(vetorTexto)

        val comprimento = sqrt(vx * vx + vy * vy + vz * vz)
        val comprimentoTexto = String.format(Locale.US, "Tamanho da Linha: %.2f", comprimento)
        ImGui.text(comprimentoTexto)

        ImGui.separator()
        if (ImGui.button("Resetar Posições")) {
            coordsA[0] = 5f; coordsA[1] = 0f; coordsA[2] = 0f
            coordsB[0] = 1f; coordsB[1] = 5f; coordsB[2] = 2f
            pontoA.x = coordsA[0]; pontoA.y = coordsA[1]; pontoA.z = coordsA[2]
            pontoB.x = coordsB[0]; pontoB.y = coordsB[1]; pontoB.z = coordsB[2]
        }

        ImGui.end()

        // --- CENA OPENGL 3D ---
        glClearColor(0.07f, 0.08f, 0.10f, 1f)
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
                kotlin.math.abs(pontoA.x), kotlin.math.abs(pontoA.y), kotlin.math.abs(pontoA.z),
                kotlin.math.abs(pontoB.x), kotlin.math.abs(pontoB.y), kotlin.math.abs(pontoB.z)
            ).maxOrNull() ?: 6f,
            6f
        ).toInt()

        desenharGrelhaXZ(maxCoord)
        desenharEixosXYZComGraduacao(maxCoord.toFloat())

        if (mostrarCaixas.get()) {
            desenharCaixaDimensional(pontoA, 0.13f, 0.82f, 0.93f)
            desenharCaixaDimensional(pontoB, 0.98f, 0.92f, 0.11f)
        }

        desenharEsferaPonto(pontoB, 0.15f, 0.98f, 0.92f, 0.11f)
        desenharEsferaPonto(pontoA, 0.15f, 0.13f, 0.82f, 0.93f)

        desenharSetaVetor(
            Vector3f(pontoA.x, pontoA.y, pontoA.z),
            Vector3f(pontoB.x, pontoB.y, pontoB.z),
            0.9f, 0.2f, 0.2f
        )

        desenharCaixaProjecao(pontoB, 0.2f, 0.9f, 0.2f)
        desenharCaixaProjecao(pontoA, 0.2f, 0.9f, 0.2f)

        if (mostrarRotulos.get()) {
            val vp = Matrix4f().mul(projection).mul(view)
            renderizarRotuloPonto(pontoA, vp, 1280f, 720f, simplificarZeros.get())
            renderizarRotuloPonto(pontoB, vp, 1280f, 720f, simplificarZeros.get())
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

// Desenha os eixos com traços e números de graduação (X e Z deitados no plano XZ)
fun desenharEixosXYZComGraduacao(limite: Float) {
    val tamanhoEixo = max(limite + 1f, 6.0f)
    glLineWidth(2.5f)
    glBegin(GL_LINES)
    glColor3f(0.8f, 0.2f, 0.2f); glVertex3f(-tamanhoEixo, 0f, 0f); glVertex3f(tamanhoEixo, 0f, 0f) // X
    glColor3f(0.2f, 0.9f, 0.2f); glVertex3f(0f, -tamanhoEixo, 0f); glVertex3f(0f, tamanhoEixo, 0f) // Y
    glColor3f(0.2f, 0.5f, 0.9f); glVertex3f(0f, 0f, -tamanhoEixo); glVertex3f(0f, 0f, tamanhoEixo) // Z
    glEnd()

    desenharCaractere3D('X', tamanhoEixo + 0.4f, 0.0f, 0.0f, 0.8f, 0.2f, 0.2f, 0f, 0f, 0f)
    desenharCaractere3D('Y', 0.0f, tamanhoEixo + 0.4f, 0.0f, 0.2f, 0.9f, 0.2f, 0f, 0f, 0f)
    desenharCaractere3D('Z', 0.0f, 0.0f, tamanhoEixo + 0.4f, 0.2f, 0.5f, 0.9f, 0f, 0f, 0f)

    val maxInt = tamanhoEixo.toInt()
    glLineWidth(1.5f)

    for (i in 1..maxInt) {
        val v = i.toFloat()

        // Eixo X (Vermelho) - Traço e Número deitados no plano XZ (rotação X = 90°)
        glColor3f(0.8f, 0.2f, 0.2f)
        glBegin(GL_LINES)
        glVertex3f(v, 0f, -0.1f); glVertex3f(v, 0f, 0.1f)
        glEnd()
        desenharNumero3D(i, v, 0.01f, 0.35f, 90f, 0f, 0f, 0.8f, 0.2f, 0.2f)

        // Eixo Y (Verde) - Mantém-se vertical
        glColor3f(0.2f, 0.9f, 0.2f)
        glBegin(GL_LINES)
        glVertex3f(-0.1f, v, 0f); glVertex3f(0.1f, v, 0f)
        glEnd()
        desenharNumero3D(i, 0.35f, v - 0.08f, 0f, 0f, 0f, 0f, 0.2f, 0.9f, 0.2f)

        // Eixo Z (Azul) - Traço e Número deitados no plano XZ (rotações X = 90° e Y = 90°)
        glColor3f(0.2f, 0.5f, 0.9f)
        glBegin(GL_LINES)
        glVertex3f(-0.1f, 0f, v); glVertex3f(0.1f, 0f, v)
        glEnd()
        desenharNumero3D(i, 0.35f, 0.01f, v, 90f, 90f, 0f, 0.2f, 0.5f, 0.9f)
    }
}

// Desenho vetorial de dígitos individuais com suporte a rotações 3D
fun desenharDigito3D(digito: Int, x: Float, y: Float, z: Float, rotX: Float, rotY: Float, rotZ: Float, r: Float, g: Float, b: Float) {
    glColor3f(r, g, b)
    glLineWidth(2.0f)
    glPushMatrix()
    glTranslatef(x, y, z)
    glRotatef(rotX, 1f, 0f, 0f)
    glRotatef(rotY, 0f, 1f, 0f)
    glRotatef(rotZ, 0f, 0f, 1f)
    val s = 0.12f

    glBegin(GL_LINES)
    when (digito) {
        0 -> {
            glVertex3f(-s, -s, 0f); glVertex3f(s, -s, 0f)
            glVertex3f(s, -s, 0f); glVertex3f(s, s, 0f)
            glVertex3f(s, s, 0f); glVertex3f(-s, s, 0f)
            glVertex3f(-s, s, 0f); glVertex3f(-s, -s, 0f)
        }
        1 -> {
            glVertex3f(0f, -s, 0f); glVertex3f(0f, s, 0f)
            glVertex3f(-s*0.5f, s*0.5f, 0f); glVertex3f(0f, s, 0f)
        }
        2 -> {
            glVertex3f(-s, s, 0f); glVertex3f(s, s, 0f)
            glVertex3f(s, s, 0f); glVertex3f(s, 0f, 0f)
            glVertex3f(s, 0f, 0f); glVertex3f(-s, 0f, 0f)
            glVertex3f(-s, 0f, 0f); glVertex3f(-s, -s, 0f)
            glVertex3f(-s, -s, 0f); glVertex3f(s, -s, 0f)
        }
        3 -> {
            glVertex3f(-s, s, 0f); glVertex3f(s, s, 0f)
            glVertex3f(s, s, 0f); glVertex3f(s, -s, 0f)
            glVertex3f(-s, 0f, 0f); glVertex3f(s, 0f, 0f)
            glVertex3f(-s, -s, 0f); glVertex3f(s, -s, 0f)
        }
        4 -> {
            glVertex3f(-s, s, 0f); glVertex3f(-s, 0f, 0f)
            glVertex3f(-s, 0f, 0f); glVertex3f(s, 0f, 0f)
            glVertex3f(s, s, 0f); glVertex3f(s, -s, 0f)
        }
        5 -> {
            glVertex3f(s, s, 0f); glVertex3f(-s, s, 0f)
            glVertex3f(-s, s, 0f); glVertex3f(-s, 0f, 0f)
            glVertex3f(-s, 0f, 0f); glVertex3f(s, 0f, 0f)
            glVertex3f(s, 0f, 0f); glVertex3f(s, -s, 0f)
            glVertex3f(s, -s, 0f); glVertex3f(-s, -s, 0f)
        }
        6 -> {
            glVertex3f(s, s, 0f); glVertex3f(-s, s, 0f)
            glVertex3f(-s, s, 0f); glVertex3f(-s, -s, 0f)
            glVertex3f(-s, -s, 0f); glVertex3f(s, -s, 0f)
            glVertex3f(s, -s, 0f); glVertex3f(s, 0f, 0f)
            glVertex3f(s, 0f, 0f); glVertex3f(-s, 0f, 0f)
        }
        7 -> {
            glVertex3f(-s, s, 0f); glVertex3f(s, s, 0f)
            glVertex3f(s, s, 0f); glVertex3f(s, -s, 0f)
        }
        8 -> {
            glVertex3f(-s, -s, 0f); glVertex3f(s, -s, 0f)
            glVertex3f(s, -s, 0f); glVertex3f(s, s, 0f)
            glVertex3f(s, s, 0f); glVertex3f(-s, s, 0f)
            glVertex3f(-s, s, 0f); glVertex3f(-s, -s, 0f)
            glVertex3f(-s, 0f, 0f); glVertex3f(s, 0f, 0f)
        }
        9 -> {
            glVertex3f(-s, -s, 0f); glVertex3f(s, -s, 0f)
            glVertex3f(s, -s, 0f); glVertex3f(s, s, 0f)
            glVertex3f(s, s, 0f); glVertex3f(-s, s, 0f)
            glVertex3f(-s, s, 0f); glVertex3f(-s, 0f, 0f)
            glVertex3f(-s, 0f, 0f); glVertex3f(s, 0f, 0f)
        }
    }
    glEnd()
    glPopMatrix()
}

// Renderiza números de múltiplos dígitos com rotação aplicada
fun desenharNumero3D(numero: Int, x: Float, y: Float, z: Float, rotX: Float, rotY: Float, rotZ: Float, r: Float, g: Float, b: Float) {
    val str = numero.toString()
    var offsetH = 0f
    for (char in str) {
        val digito = char.toString().toInt()
        // Ajusta o espaçamento dependendo se está deitado ou vertical
        val px = if (rotY != 0f) x else x + offsetH
        val pz = if (rotY != 0f) z + offsetH else z
        desenharDigito3D(digito, px, y, pz, rotX, rotY, rotZ, r, g, b)
        offsetH += 0.3f
    }
}

fun desenharCaractere3D(char: Char, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, rotX: Float, rotY: Float, rotZ: Float) {
    glColor3f(r, g, b)
    glLineWidth(3.0f)
    glPushMatrix()
    glTranslatef(x, y, z)
    glRotatef(rotX, 1f, 0f, 0f)
    glRotatef(rotY, 0f, 1f, 0f)
    glRotatef(rotZ, 0f, 0f, 1f)
    val s = 0.25f

    glBegin(GL_LINES)
    when (char) {
        'X' -> {
            glVertex3f(-s, -s, 0f); glVertex3f(s, s, 0f)
            glVertex3f(-s, s, 0f); glVertex3f(s, -s, 0f)
        }
        'Y' -> {
            glVertex3f(-s, s, 0f); glVertex3f(0f, 0f, 0f)
            glVertex3f(s, s, 0f); glVertex3f(0f, 0f, 0f)
            glVertex3f(0f, 0f, 0f); glVertex3f(0f, -s, 0f)
        }
        'Z' -> {
            glVertex3f(-s, s, 0f); glVertex3f(s, s, 0f)
            glVertex3f(s, s, 0f); glVertex3f(-s, -s, 0f)
            glVertex3f(-s, -s, 0f); glVertex3f(s, -s, 0f)
        }
    }
    glEnd()
    glPopMatrix()
}

fun desenharCaixaDimensional(p: Ponto3D, r: Float, g: Float, b: Float) {
    glColor3f(r, g, b)
    glLineWidth(1.5f)
    glEnable(GL_LINE_STIPPLE)
    glLineStipple(1, 0x0F0F.toShort())

    glBegin(GL_LINES)
    val x = p.x
    val y = p.y
    val z = p.z

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

fun desenharGrelhaXZ(limite: Int) {
    glColor4f(0.18f, 0.19f, 0.22f, 0.5f)
    glLineWidth(1f)
    glBegin(GL_LINES)
    for (i in -limite..limite) {
        glVertex3f(i.toFloat(), 0f, -limite.toFloat()); glVertex3f(i.toFloat(), 0f, limite.toFloat())
        glVertex3f(-limite.toFloat(), 0f, i.toFloat()); glVertex3f(limite.toFloat(), 0f, i.toFloat())
    }
    glEnd()
}

fun desenharEsferaPonto(p: Ponto3D, raio: Float, r: Float, g: Float, b: Float) {
    glColor3f(r, g, b)
    glPushMatrix()
    glTranslatef(p.x, p.y, p.z)
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
    glColor3f(r, g, b)
    glLineWidth(3f)

    glBegin(GL_LINES)
    glVertex3f(inicio.x, inicio.y, inicio.z)
    glVertex3f(fim.x, fim.y, fim.z)
    glEnd()

    val direcao = Vector3f(fim).sub(inicio)
    val comprimento = direcao.length()

    if (comprimento > 0.001f) {
        direcao.normalize()

        val pontoBaseSeta = Vector3f(fim).sub(Vector3f(direcao).mul(0.45f))

        val perp = Vector3f()
        if (kotlin.math.abs(direcao.y) > 0.99f) {
            perp.set(1f, 0f, 0f)
        } else {
            perp.set(-direcao.z, 0f, direcao.x).normalize()
        }

        val tamanhoAleta = 0.22f
        val offsetLateral = perp.mul(tamanhoAleta)

        val aleta1 = Vector3f(pontoBaseSeta).add(offsetLateral)
        val aleta2 = Vector3f(pontoBaseSeta).sub(offsetLateral)

        val perp2 = Vector3f(direcao).cross(perp).normalize()
        val offsetProfundidade = perp2.mul(tamanhoAleta)
        val aleta3 = Vector3f(pontoBaseSeta).add(offsetProfundidade)
        val aleta4 = Vector3f(pontoBaseSeta).sub(offsetProfundidade)

        glBegin(GL_LINES)
        glVertex3f(fim.x, fim.y, fim.z); glVertex3f(aleta1.x, aleta1.y, aleta1.z)
        glVertex3f(fim.x, fim.y, fim.z); glVertex3f(aleta2.x, aleta2.y, aleta2.z)
        glVertex3f(fim.x, fim.y, fim.z); glVertex3f(aleta3.x, aleta3.y, aleta3.z)
        glVertex3f(fim.x, fim.y, fim.z); glVertex3f(aleta4.x, aleta4.y, aleta4.z)
        glEnd()
    }
}

fun desenharCaixaProjecao(p: Ponto3D, r: Float, g: Float, b: Float) {
    glColor3f(r, g, b)
    glLineWidth(2f)
    glBegin(GL_LINES)
    glVertex3f(p.x, 0f, p.z); glVertex3f(p.x, p.y, p.z)
    glEnd()
}

fun renderizarRotuloPonto(p: Ponto3D, viewProjection: Matrix4f, larguraJanela: Float, alturaJanela: Float, simplificar: Boolean) {
    val clipCoords = Vector4f(p.x, p.y + 0.35f, p.z, 1.0f).mul(viewProjection)
    if (clipCoords.w > 0.0f) {
        val invW = 1.0f / clipCoords.w
        val ndcX = clipCoords.x * invW
        val ndcY = clipCoords.y * invW

        val screenX = (larguraJanela * 0.5f * ndcX) + (larguraJanela * 0.5f)
        val screenY = (alturaJanela * 0.5f * (-ndcY)) + (alturaJanela * 0.5f)

        val coordsStr = formatarCoordenadas(p.x, p.y, p.z, simplificar)
        val textoCoord = "${p.nome}: $coordsStr"

        val drawList = ImGui.getBackgroundDrawList()
        drawList.addText(screenX - 35f, screenY - 15f, 0xFFFFFFFF.toInt(), textoCoord)
    }
}