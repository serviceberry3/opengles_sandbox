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

import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class Square {
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint textPaint;
    private Drawable background;
    private FloatBuffer vertexBuffer;   // buffer holding the vertices

    private float vertices[] = {
                    -1f, -0.5f,  0.0f,        // V1 - bottom left
                    -1f,  0.5f,  0.0f,        // V2 - top left
                    0f, -0.5f,  0.0f,        // V3 - bottom right
                    0f,  0.5f,  0.0f         // V4 - top right
    };

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

    public Square() {
        // a float has 4 bytes so we allocate for each coordinate 4 bytes
        ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);

        vertexByteBuffer.order(ByteOrder.nativeOrder());

        // allocates the memory from the byte buffer
        vertexBuffer = vertexByteBuffer.asFloatBuffer();

        // fill the vertexBuffer with the vertices
        vertexBuffer.put(vertices);

        // set the cursor position to the beginning of the buffer
        vertexBuffer.position(0);

        vertexByteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
        vertexByteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = vertexByteBuffer.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);
    }


    //draw method for square with gl context
    public void draw(GL10 gl) {
        //bind the previously generated texture
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        //enable OpenGL to use a vertex array for rendering (contains vertices for square).
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
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
}


