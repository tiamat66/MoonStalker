package com.robic.zoran.moonstalker;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Telescope3D
{
  private FloatBuffer vertexBuffer;  // Buffer for vertex-array
  private int numFaces = 6;

  private float[][] colors = {  // Colors of the 6 faces
    {1.0f, 0.5f, 0.0f, 1.0f},  // 0. orange
    {1.0f, 0.0f, 1.0f, 1.0f},  // 1. violet
    {0.0f, 1.0f, 0.0f, 1.0f},  // 2. green
    {0.0f, 0.0f, 1.0f, 1.0f},  // 3. blue
    {1.0f, 0.0f, 0.0f, 1.0f},  // 4. red
    {1.0f, 1.0f, 0.0f, 1.0f}   // 5. yellow
  };

  private float[] vertices = {  // Vertices of the 6 faces
    // FRONT
    -1.0f, -1.0f, 1.0f,  // 0. left-bottom-front
    1.0f, -1.0f, 1.0f,  // 1. right-bottom-front
    -1.0f, 1.0f, 1.0f,  // 2. left-top-front
    1.0f, 1.0f, 1.0f,  // 3. right-top-front
    // BACK
    1.0f, -1.0f, -1.0f,  // 6. right-bottom-back
    -1.0f, -1.0f, -1.0f,  // 4. left-bottom-back
    1.0f, 1.0f, -1.0f,  // 7. right-top-back
    -1.0f, 1.0f, -1.0f,  // 5. left-top-back
    // LEFT
    -1.0f, -1.0f, -1.0f,  // 4. left-bottom-back
    -1.0f, -1.0f, 1.0f,  // 0. left-bottom-front
    -1.0f, 1.0f, -1.0f,  // 5. left-top-back
    -1.0f, 1.0f, 1.0f,  // 2. left-top-front
    // RIGHT
    1.0f, -1.0f, 1.0f,  // 1. right-bottom-front
    1.0f, -1.0f, -1.0f,  // 6. right-bottom-back
    1.0f, 1.0f, 1.0f,  // 3. right-top-front
    1.0f, 1.0f, -1.0f,  // 7. right-top-back
    // TOP
    -1.0f, 1.0f, 1.0f,  // 2. left-top-front
    1.0f, 1.0f, 1.0f,  // 3. right-top-front
    -1.0f, 1.0f, -1.0f,  // 5. left-top-back
    1.0f, 1.0f, -1.0f,  // 7. right-top-back
    // BOTTOM
    -1.0f, -1.0f, -1.0f,  // 4. left-bottom-back
    1.0f, -1.0f, -1.0f,  // 6. right-bottom-back
    -1.0f, -1.0f, 1.0f,  // 0. left-bottom-front
    1.0f, -1.0f, 1.0f   // 1. right-bottom-front
  };

  // Constructor - Set up the buffers
  public Telescope3D()
  {
    // Setup vertex-array buffer. Vertices in float. An float has 4 bytes
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder()); // Use native byte order
    vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
    vertexBuffer.put(vertices);         // Copy data into buffer
    vertexBuffer.position(0);           // Rewind
  }

  // Draw the shape
  public void draw(GL10 gl)
  {
    gl.glFrontFace(GL10.GL_CCW);    // Front face in counter-clockwise orientation
    gl.glEnable(GL10.GL_CULL_FACE); // Enable cull face
    gl.glCullFace(GL10.GL_BACK);    // Cull the back face (don't display)

    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

    // Render all the faces
    for (int face = 0; face < numFaces; face++) {
      // Set the color for each of the faces
      gl.glColor4f(colors[face][0], colors[face][1], colors[face][2], colors[face][3]);
      // Draw the primitive from the vertex-array directly
      gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, face * 4, 4);
    }
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisable(GL10.GL_CULL_FACE);
  }
}

class Pyramid
{
  private FloatBuffer vertexBuffer;  // Buffer for vertex-array
  private FloatBuffer colorBuffer;   // Buffer for color-array
  private ByteBuffer indexBuffer;    // Buffer for index-array

  private float[] vertices = { // 5 vertices of the pyramid in (x,y,z)
    -1.0f, -1.0f, -1.0f,  // 0. left-bottom-back
    1.0f, -1.0f, -1.0f,  // 1. right-bottom-back
    1.0f, -1.0f, 1.0f,  // 2. right-bottom-front
    -1.0f, -1.0f, 1.0f,  // 3. left-bottom-front
    0.0f, 1.0f, 0.0f   // 4. top
  };

  private float[] colors = {  // Colors of the 5 vertices in RGBA
    0.0f, 0.0f, 1.0f, 1.0f,  // 0. blue
    0.0f, 1.0f, 0.0f, 1.0f,  // 1. green
    0.0f, 0.0f, 1.0f, 1.0f,  // 2. blue
    0.0f, 1.0f, 0.0f, 1.0f,  // 3. green
    1.0f, 0.0f, 0.0f, 1.0f   // 4. red
  };

  private byte[] indices = { // Vertex indices of the 4 Triangles
    2, 4, 3,   // front face (CCW)
    1, 4, 2,   // right face
    0, 4, 1,   // back face
    4, 0, 3    // left face
  };

  // Constructor - Set up the buffers
  public Pyramid()
  {
    // Setup vertex-array buffer. Vertices in float. An float has 4 bytes
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
    vbb.order(ByteOrder.nativeOrder()); // Use native byte order
    vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
    vertexBuffer.put(vertices);         // Copy data into buffer
    vertexBuffer.position(0);           // Rewind

    // Setup color-array buffer. Colors in float. An float has 4 bytes
    ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
    cbb.order(ByteOrder.nativeOrder());
    colorBuffer = cbb.asFloatBuffer();
    colorBuffer.put(colors);
    colorBuffer.position(0);

    // Setup index-array buffer. Indices in byte.
    indexBuffer = ByteBuffer.allocateDirect(indices.length);
    indexBuffer.put(indices);
    indexBuffer.position(0);
  }

  // Draw the shape
  public void draw(GL10 gl)
  {
    gl.glFrontFace(GL10.GL_CCW);  // Front face in counter-clockwise orientation

    // Enable arrays and define their buffers
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);

    gl.glDrawElements(GL10.GL_TRIANGLES, indices.length, GL10.GL_UNSIGNED_BYTE,
      indexBuffer);

    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
  }
}