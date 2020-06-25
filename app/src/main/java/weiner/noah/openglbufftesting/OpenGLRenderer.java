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
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

/*
The model matrix. This matrix is used to place a model somewhere in the “world”. For example, if you have a model of a car and you want it located 1000 meters to the east, you will use the model matrix to do this.
The view matrix. This matrix represents the camera. If we want to view our car which is 1000 meters to the east, we’ll have to move ourselves 1000 meters to the east as well (another way of thinking about it is that we remain stationary, and the rest of the world moves 1000 meters to the west). We use the view matrix to do this.
The projection matrix. Since our screens are flat, we need to do a final transformation to “project” our view onto our screen and get that nice 3D perspective. This is what the projection matrix is used for.
 */

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

    private long time;

    //data for projection and camera view
    //vPMatrix is abbreviation for "Model View Projection Matrix." Use this matrix if we want to just combine the matrices by matrix multiplication
    private final float[] vPMatrix = new float[16];

    //store the projection matrix. This is used to project scene onto a 2D viewport.
    private final float[] projectionMatrix = new float[16];

    //store the view matrix. This can be thought of as our camera. The matrix transforms world space to eye space; it positions things relative to our eye.
    private final float[] viewMatrix = new float[16];

    //store the model matrix. This is used
    private float[] mModelMatrix = new float[16];

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

    //this method transitions the OpenGL context between a few states This is called whenever the surface changes;
    // for example, when switching from portrait to landscape. It is also called after the surface has been created.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //Projection matrix work--since only need to reset projection matrix whenever screen we're projecting onto has changed, this is good place

        //reset the current viewport. Set the openGL viewport to same size as the surface
        GLES20.glViewport(0, 0, width, height);

        //avoid dividing by 0
        if (height==0) {
            height = 1;
        }

        //create new perspective projection matrix. The height will stay the same while width will vary by aspect ratio.
        float ratio = (float) width/height;
        float left = -ratio;
        float right = ratio;
        float bottom = -1.0f;
        float top = 1.0f;
        float near = 3.0f; //could try 1.0f?
        float far = 7.0f;  //could try 10.0f?


        //this projection matrix is applied to object coordinates in onDrawFrame()
        Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);

        //select the projection matrix
        //gl.glMatrixMode(GL10.GL_PROJECTION);

        //reset the projection matrix
        //gl.glLoadIdentity();

        //calculate aspect ratio of the window
        //GLU.gluPerspective(gl, 45.0f, ratio, 0.1f, 100.0f);

        //select the modelview matrix
        //gl.glMatrixMode(GL10.GL_MODELVIEW);

        //reset modelview matrix
        //gl.glLoadIdentity();
    }

    //This method is called when the surface is first created. It will also be called if we lose our surface context and it is later recreated by the system.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //set background clear color to purple
        GLES20.glClearColor(0.5f, 0, 0.5f, 1f);

        //insantiate a triangle and a square
        mTriangle = new Triangle();
        mSquare = new Square();


        //load the texture for the square, provide the context to our renderer so we can load up the texture at startup
        mSquare.loadGLTexture(gl, this.myContext);

        GLES20.glEnable(GLES20.GL_TEXTURE_2D); //enable texture mapping (NEW)

        gl.glShadeModel(GL10.GL_SMOOTH); //enable smooth shading

        //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); //black background

        //GLES20.glClearDepthf(1.0f); //depth buffer setup

        GLES20.glEnable(GLES20.GL_DEPTH_TEST); //enables depth testing

        GLES20.glDepthFunc(GLES20.GL_LEQUAL); //the type of depth testing to do

        GLES20.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT, GLES20.GL_NICEST);


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

    //This is called whenever it’s time to draw a new frame.
    @Override
    public void onDrawFrame(GL10 gl) {
        // clear the color buffer (bitmaps) -- clear screen and depth buffer
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float[] scratch = new float[16];

        //create a rotation transformation for the triangle
        time = SystemClock.uptimeMillis() % 4000L;

        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(myContext, String.format("%d", time), Toast.LENGTH_SHORT).show();
            }
        });

        //float angle = 0.090f * ((int) time);
        float angle = (360.0f / 4000.0f) * ((int) time);

        //set up the camera

        //Position the eye behind the origin
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = -7.0f; //WAS 1.5

        //We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -1.5f; //WAS -5

        //Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        //set camera position. NOTE: in Opengl 1, a ModelView matrix is used (a combo of a model and a view matrix). In 2.0, can keep track of these matrices separately.
        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        //set model matrix to identity matrix
        Matrix.setIdentityM(mModelMatrix, 0);

        //set rotation matrix using angle calculated
        Matrix.setRotateM(rotationMatrix, 0, angle, 0, 0, 1);

        //multiply model matrix (identity matrix) by rotation matrix
        Matrix.multiplyMM(vPMatrix, 0, mModelMatrix, 0, rotationMatrix, 0);

        //calculate projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, vPMatrix, 0);

        //combine rotation matrix with the projection and camera view
        //note that vPMatrix factor MUST BE FIRST in order for matrix multiplication product to be correct
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, viewMatrix, 0);

        //draw the triangle with the final matrix
        mTriangle.draw(scratch);

        //draw the square with the final matrix
        mSquare.draw(scratch);

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

        if (shader!=0) {
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode);

            //compile the shader
            GLES20.glCompileShader(shader);

            //get the compilation status
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            //if compilation failed, delete the shader
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        if (shader==0) {
            throw new RuntimeException("Error creating vertex shader.");
        }

        return shader;
    }
}
