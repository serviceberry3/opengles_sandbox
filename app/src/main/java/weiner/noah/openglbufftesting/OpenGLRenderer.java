package weiner.noah.openglbufftesting;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLRenderer implements GLSurfaceView.Renderer {
    Context myContext;
    Activity myActivity;
    int[] textures = new int[5];
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint textPaint;
    private Drawable background;
    private Triangle mTriangle;
    private Square mSquare;

    //data for projection and camera view
    //vPMatrix is abbreviation for "Model View Projection Matrix"
    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    //make a rotation matrix
    private float[] rotationMatrix = new float[16];


    //text render to texture vars
    // RENDER TO TEXTURE VARIABLES
    int[] fb, depthRb, renderTex; // the framebuffer, the renderbuffer and the texture to render
    int texW = 480 * 2;           // the texture's width
    int texH = 800 * 2;           // the texture's height
    IntBuffer texBuffer;          //  Buffer to store the texture

    public OpenGLRenderer(Context context, Activity activity) {
        //provide the application context to the square object because the obj itself loads the texture and needs to know the path to the bitmap
        myContext = context;
        myActivity = activity;
    }

    //this method transitions the OpenGL context between a few states
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //reset the current viewport
        GLES20.glViewport(0, 0, width, height);

        //this projection matrix is applied to object coordinates in onDrawFrame()
        //Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

        //avoid dividing by 0
        if (height==0) {
            height = 1;
        }

        float ratio = (float) width/height;

        //select the projection matrix
        gl.glMatrixMode(GL10.GL_PROJECTION);

        //reset the projection matrix
        gl.glLoadIdentity();

        //calculate aspect ratio of the window
        GLU.gluPerspective(gl, 45.0f, ratio, 0.1f, 100.0f);

        //select the modelview matrix
        gl.glMatrixMode(GL10.GL_MODELVIEW);

        //reset modelview matrix
        gl.glLoadIdentity();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        mTriangle = new Triangle();
        mSquare = new Square();

        //load the texture for the square, provide the context to our renderer so we can load up the texture at startup
        mSquare.loadGLTexture(gl, this.myContext);

        GLES20.glEnable(GLES20.GL_TEXTURE_2D); //enable texture mapping (NEW)

        gl.glShadeModel(GL10.GL_SMOOTH); //enable smooth shading

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); //black background

        GLES20.glClearDepthf(1.0f); //depth buffer setup

        GLES20.glEnable(GLES20.GL_DEPTH_TEST); //enables depth testing

        GLES20.glDepthFunc(GLES20.GL_LEQUAL); //the type of depth testing to do

        GLES20.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT, GLES20.GL_NICEST);


       //GLES20.glClearColor(0.5f, 0, 0.5f, 1f);


/*
        bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);

        canvas = new Canvas(bitmap);
        bitmap.eraseColor(0);

        background = myContext.getResources().getDrawable(R.drawable.ic_launcher_background);

        background.setBounds(0, 0, 256, 256);

        background.draw(canvas);

        textPaint = new Paint();
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
        textPaint.setARGB(0xff, 0x00, 0x00, 0x00);

        canvas.drawText("Hello World", 16, 112, textPaint);
 */
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // clear the color buffer (bitmaps) -- clear screen and depth buffer
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


        float[] scratch = new float[16];

        //create a rotation transformation for the triangle
        long time = SystemClock.uptimeMillis() % 4000L;
        float angle = 0.090f * ((int) time);

        Matrix.setRotateM(rotationMatrix, 0, angle, 0, 0, -1.0f);

        //combine rotation matrix with the projection and camera view
        //note that vPMatrix factor MUST BE FIRST in order for matrix multiplication product to be correct
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0);

        //set camera position
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 1.0f);

        //calculate projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        //reset the Modelview Matrix
        gl.glLoadIdentity();

        //drawing -- move 5 units INTO the screen is the same as moving the camera 5 units (UNITS, NOT PIXELS) away
        gl.glTranslatef(0.0f, 0.0f, -5.0f);

        //scale the image by 1/2 (z factor doesn't do anything here)
        gl.glScalef(0.5f, 0.5f, 0.5f);

        //draw the triangle
        mTriangle.draw(scratch);

        //draw the square
        //mSquare.draw(gl);


/*
        // generate one texture pointer...
        gl.glGenTextures(1, textures, 0);
        //...and bind it to our array
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        // create Nearest Filtered Texture
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        // different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

        // use the Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);



        float textureCoordinates[] = {0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f };

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(textures.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        FloatBuffer textureBuffer = byteBuf.asFloatBuffer();
        textureBuffer.put(textureCoordinates);
        textureBuffer.position(0);
         */


        // clean up
        //bitmap.recycle();
    }

    public static int loadShader(int type, String shaderCode) {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;

    }
}
