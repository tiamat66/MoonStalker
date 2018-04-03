package com.robic.zoran.moonstalker;

import android.content.Context;
import android.graphics.Canvas;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.view.View;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class View3D extends GLSurfaceView
{
  Context context;

  public View3D(Context act)
  {
    super(act);

    this.setRenderer(new Renderer3D(act));
    context = act;
  }
}

class Renderer3D implements GLSurfaceView.Renderer
{
  Context context;
//  Triangle triangle;
//  Square   quad;

  private Pyramid pyramid;
  private Telescope3D cube;

  private static float anglePyramid = 0; // Rotational angle in degree for pyramid (NEW)
  private static float angleCube = 0;    // Rotational angle in degree for cube (NEW)
  private static float speedPyramid = 2.0f; // Rotational speed for pyramid (NEW)
  private static float speedCube = -1.5f;   // Rotational speed for cube (NEW)

  // Rotational angle and speed
//  private float angleTriangle = 0.0f;
//  private float angleQuad = 0.0f;
//  private float speedTriangle = 0.5f;
//  private float speedQuad = -0.4f;

  Renderer3D(Context act)
  {
    context = act;
    pyramid = new Pyramid();
    cube = new Telescope3D();
//    triangle = new Triangle();
//    quad = new Square();
  }

  // Call back when the surface is first created or re-created
  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config)
  {
    gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);  // Set color's clear-value to black
    gl.glClearDepthf(1.0f);            // Set depth's clear-value to farthest
    gl.glEnable(GL10.GL_DEPTH_TEST);   // Enables depth-buffer for hidden surface removal
    gl.glDepthFunc(GL10.GL_LEQUAL);    // The type of depth testing to do
    gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);  // nice perspective view
    gl.glShadeModel(GL10.GL_SMOOTH);   // Enable smooth shading of color
    gl.glDisable(GL10.GL_DITHER);      // Disable dithering for better performance

    // You OpenGL|ES initialization code here
    // ......
  }

  // Call back after onSurfaceCreated() or whenever the window's size changes
  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height)
  {
    if (height == 0) height = 1;   // To prevent divide by zero
    float aspect = (float) width / height;

    // Set the viewport (display area) to cover the entire window
    gl.glViewport(0, 0, width, height);

    // Setup perspective projection, with aspect ratio matches viewport
    gl.glMatrixMode(GL10.GL_PROJECTION); // Select projection matrix
    gl.glLoadIdentity();                 // Reset projection matrix
    // Use perspective projection
    GLU.gluPerspective(gl, 45, aspect, 0.1f, 100.f);

    gl.glMatrixMode(GL10.GL_MODELVIEW);  // Select model-view matrix
    gl.glLoadIdentity();                 // Reset

    // You OpenGL|ES display re-sizing code here
    // ......
  }

  // Call back to draw the current frame.
  @Override
  public void onDrawFrame(GL10 gl)
  {
    // Clear color and depth buffers using clear-value set earlier
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

    // ----- Render the Pyramid -----
    gl.glLoadIdentity();                 // Reset the model-view matrix
    gl.glTranslatef(-1.5f, 0.0f, -6.0f); // Translate left and into the screen
    gl.glRotatef(anglePyramid, 0.1f, 1.0f, -0.1f); // Rotate
    pyramid.draw(gl);                              // Draw the pyramid

    // ----- Render the Color Cube -----
    gl.glLoadIdentity();                // Reset the model-view matrix
    gl.glTranslatef(1.5f, 0.0f, -6.0f); // Translate right and into the screen
    gl.glScalef(0.8f, 0.8f, 0.8f);      // Scale down
    gl.glRotatef(angleCube, 1.0f, 1.0f, 1.0f); // rotate about the axis (1,1,1)
    cube.draw(gl);                      // Draw the cube

    // Update the rotational angle after each refresh
    anglePyramid += speedPyramid;
    angleCube += speedCube;

//    gl.glLoadIdentity();                 // Reset model-view matrix
//    gl.glTranslatef(-1.5f, 0.0f, -6.0f); // Translate left and into the screen
//    gl.glRotatef(angleTriangle, 0.0f, 1.0f, 0.0f); // Rotate the triangle about the y-axis
//    triangle.draw(gl);                   // Draw triangle
//
//    // Translate right, relative to the previous translation
//    gl.glLoadIdentity();                 // Reset the mode-view matrix (NEW)
//    gl.glTranslatef(1.5f, 0.0f, -6.0f);  // Translate right and into the screen (NEW)
//    gl.glRotatef(angleQuad, 1.0f, 0.0f, 0.0f); // Rotate the square about the x-axis (NEW)
//    quad.draw(gl);                       // Draw quad
//
//    // Update the rotational angle after each refresh
//    angleTriangle += speedTriangle;
//    angleQuad += speedQuad;
  }
}

/*
 * A triangle with 3 vertices.
 */
class Triangle
{
  private FloatBuffer vertexBuffer;  // Buffer for vertex-array
  private ByteBuffer indexBuffer;    // Buffer for index-array
  private FloatBuffer colorBuffer;   // Buffer for color-array

  private float[] vertices = {  // Vertices of the triangle
    0.0f, 1.0f, 0.0f,           // 0. top
    -1.0f, -1.0f, 0.0f,         // 1. left-bottom
    1.0f, -1.0f, 0.0f           // 2. right-bottom
  };
  private byte[] indices = {0, 1, 2}; // Indices to above vertices (in CCW)
  private float[] colors = { // Colors for the vertices
    1.0f, 0.0f, 0.0f, 1.0f,  // Red
    0.0f, 1.0f, 0.0f, 1.0f,  // Green
    0.0f, 0.0f, 1.0f, 1.0f   // Blue
  };

  // Constructor - Setup the data-array buffers
  Triangle()
  {
    // Setup vertex-array buffer. Vertices in float. A float has 4 bytes.
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder()); // Use native byte order
    vertexBuffer = vbb.asFloatBuffer(); // Convert byte buffer to float
    vertexBuffer.put(vertices);         // Copy data into buffer
    vertexBuffer.position(0);           // Rewind

    // Setup color-array buffer. Colors in float. A float has 4 bytes
    ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
    cbb.order(ByteOrder.nativeOrder()); // Use native byte order
    colorBuffer = cbb.asFloatBuffer();  // Convert byte buffer to float
    colorBuffer.put(colors);            // Copy data into buffer
    colorBuffer.position(0);            // Rewind

    // Setup index-array buffer. Indices in byte.
    indexBuffer = ByteBuffer.allocateDirect(indices.length);
    indexBuffer.put(indices);
    indexBuffer.position(0);
  }

  // Render this shape
  void draw(GL10 gl)
  {
    // Enable vertex-array and define the buffers
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);               // Enable color-array
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer); // Define color-array buffer

    // Draw the primitives via index-array
    gl.glDrawElements(GL10.GL_TRIANGLES, indices.length, GL10.GL_UNSIGNED_BYTE, indexBuffer);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);   // Disable color-array
  }
}

/*
 * A square drawn in 2 triangles (using TRIANGLE_STRIP).
 */
class Square
{
  private FloatBuffer vertexBuffer;  // Buffer for vertex-array

  private float[] vertices = {  // Vertices for the square
    -1.0f, -1.0f, 0.0f,  // 0. left-bottom
    1.0f, -1.0f, 0.0f,  // 1. right-bottom
    -1.0f, 1.0f, 0.0f,  // 2. left-top
    1.0f, 1.0f, 0.0f   // 3. right-top
  };

  // Constructor - Setup the vertex buffer
  Square()
  {
    // Setup vertex array buffer. Vertices in float. A float has 4 bytes
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder()); // Use native byte order
    vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
    vertexBuffer.put(vertices);         // Copy data into buffer
    vertexBuffer.position(0);           // Rewind
  }

  // Render the shape
  void draw(GL10 gl)
  {
    // Enable vertex-array and define its buffer
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glColor4f(0.5f, 0.6f, 1.0f, 1.0f);      // Set the current
    // Draw the primitives from the vertex-array directly
    gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
  }
}