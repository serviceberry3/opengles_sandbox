package weiner.noah.openglbufftesting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class Square {
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint textPaint;
    private Drawable background;
    private FloatBuffer vertexBuffer;   // buffer holding the vertices
    private ShortBuffer drawListBuffer;
    private final int mProgram;
    private int positionHandle;
    private int colorHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;

    /*
    private float vertices[] = {
                    -1f, -0.5f,  0.0f,        // V1 - bottom left
                    -1f,  0.5f,  0.0f,        // V2 - top left
                    0f, -0.5f,  0.0f,        // V3 - bottom right
                    0f,  0.5f,  0.0f         // V4 - top right
    };
     */

    private float vertices[] = {
            -0.5f, 0.5f, 0.0f,   //top left
            -0.5f, -0.5f, 0.0f,  //bottom left
            0.5f, -0.5f, 0.0f,   //bottom right
            0.5f, 0.5f, 0.0f   //top right
    };

    private final int vertexCount = vertices.length / COORDS_PER_VERTEX;

    //how much memory space each vertex takes up
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private short drawOrder[] = {0, 1, 2, 0, 2, 3}; //order to draw vertices

    //buffer holding the texture
    private FloatBuffer textureBuffer;
    private float[] texture = {
            //mapping coordinates for vertices
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    //the texture pointer array, where openGL will store names of textures we'll use in our app
    private int[] textures = new int[1];

    // Set color with red, green, blue and alpha (opacity) values
    float[] color = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    public Square() {
        //load the vertex shader
        int vertexShader = OpenGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);

        //load the fragment shader
        int fragmentShader = OpenGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        //create empty openGL ES Program
        mProgram = GLES20.glCreateProgram();

        //add the vertex shader to program
        GLES20.glAttachShader(mProgram, vertexShader);

        //add fragment shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);

        //link the program, create OpenGL ES program executable
        GLES20.glLinkProgram(mProgram);

        // a float has 4 bytes so we allocate for each coordinate 4 bytes
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);

        vertexByteBuffer.order(ByteOrder.nativeOrder());

        // allocates the memory from the byte buffer
        vertexBuffer = vertexByteBuffer.asFloatBuffer();

        // fill the vertexBuffer with the vertices
        vertexBuffer.put(vertices);

        // set the cursor position to the beginning of the buffer
        vertexBuffer.position(0);


        //initialize byte buffer for the draw list
        vertexByteBuffer = ByteBuffer.allocateDirect(drawOrder.length * 2);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        drawListBuffer = vertexByteBuffer.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


        vertexByteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = vertexByteBuffer.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);
    }


    //draw method for square with gl context
    public void draw(float[] mvpMatrix) {
        //add the program to the OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        //get the vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        //enable openGL to read from FloatBuffer that contains the triangle's vertices' coords and to understand that there's a triangle there
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnable(GL10.GL_TEXTURE_COORD_ARRAY);

        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);


        //point to our vertex buffer
        //tell OpenGL to use the vertexBuffer to extract the vertices from
        //@param size = 3 represents number of vertices in the buffer
        //@param what type of data the buffer holds
        //@param the offset in the array used for the vertices (in this case they follow each other, no extra data stored)
        //@param the buffer containing the vertices
        //prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        //GLES20.glClearColor(0.0f, 0.0f,0.0f,0.5f);

        //clear the color buffer (bitmaps) -- clear screen and depth buffer
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //get fragment shader's vColor member
        colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        //set color for triangle -- values of RGB floats are between 0 and 1 inclusif
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        //get shape's transformation matrix
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        //pass projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0);

        //draw the vertices as a triangle strip
        //tells OpenGL to draw triangle strips found in buffer provided, starting with first element. Also "count" is how many vertices there are
        //GLES20.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, triangleCoords.length / COORDS_PER_VERTEX);

        //draw triangle -- google version
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        //disable vertex array (disable client state before leaving)
        GLES20.glDisableVertexAttribArray(positionHandle);







/*
        //bind the previously generated texture
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        //enable OpenGL to use a vertex array for rendering (contains vertices for square).
        GLES20.glEnableVertexAttribArray(positionHandle);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        //set the face rotation
        gl.glFrontFace(GL10.GL_CW);

        gl.glClearColor(0.0f, 0.0f,0.0f,0.5f);

        // clear the color buffer (bitmaps) -- clear screen and depth buffer
        //gl.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //set the colour for the square
        gl.glColor4f(0.0f, 1.0f, 0.0f, 0.5f);

        // Point to our vertex buffer -- tells openGL renderer from where to take the vertices and of what type they are
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer); //provides OpenGL context with the texture coordinates


        // Draw the vertices as triangle strip
        //tells openGL to draw the primitive specified: a triangle strip. Takes vertices from previously set buffer and follows rules for drawing strips (special order)
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);


        //Disable the client state before leaving -- disables the state of rendering from an array containing vertices
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        //these enable and disable are like begin...end statements in a program. We enter subroutines in the OpenGL renderer. Once we entered a routine, we set up vars (vertex buff, color, etc) and execute
        //other subroutines (drawvertices). Once done, we exit the subroutine. We work in isolation inside the renderer.

 */
    }

    public void loadGLTexture(GL10 gl, Context context) {
        //loading texture -- loads Android bitmap. It's best if the bitmap is square, because that helps a lot with scaling. Make sure bitmaps for textures are squares;
        //if not, make sure width and height are pwrs of 2
        //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.android);

        bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);

        canvas = new Canvas(bitmap);
        bitmap.eraseColor(0);

        background = context.getResources().getDrawable(R.drawable.ic_launcher_background);

        background.setBounds(0, 0, 256, 256);

        background.draw(canvas);

        //create a new paint object
        textPaint = new Paint();

        //sets the size of the text to display
        textPaint.setTextSize(45);

        //set antialiasing bit in the flags, which smooths out edges of what is being drawn
        textPaint.setAntiAlias(true);

        //set the color of the paint
        textPaint.setARGB(0xff, 0x00, 0x00, 0xdd);

        canvas.drawText("Hello World", 14, 135, textPaint); //WAS x:16, y:112

        //generate one texture ptr/names for textures (actually generates an int)
        gl.glGenTextures(1, textures, 0);

        //and bind it to our array -- binds texture with newly generated name. Meaning, anything using textures in this subroutine will use the bound texture.
        //Basically activates the texture. If we had had multiple textures and multiples squares for them, would have had to bind (activate) the appropriate textures
        //for each square just before they were used
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        //create nearest filtered texture -- tells openGL what types of filters to use when it needs to shrink or expand texture to cover the square
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        //use Android GLUtils to specify a 2d texture image from our bitmap. Creates the image (texture) internally in its native format based on our bitmap
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

        //clean up -- free up memory used by original bitmap
        bitmap.recycle();
    }

    private final String vertexShaderCode =
            //this matrix member var provides a hook to manipulate the coords of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    //the matrix must be included as modifier of gl_Position
                    //NOTE: the uMVPMatrix factor MUST BE FIRST in order for matrix multiplication product to be correct
                    "gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    //use to access and set the view transformation
    private int vPMatrixHandle;

    private final String fragmentShaderCode =
            "precision mediump float;" +    //how much precision GPU uses when calculating floats
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "gl_FragColor = vColor;" +
                    "}";
}


