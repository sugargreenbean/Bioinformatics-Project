////////////////////////////////////////////
 
// This file is in completed form //
 
///////////////////////////////////////////


import java.util.*;

public class CartesianCoord {

    public double x,y,z;

    public CartesianCoord(double newX, double newY, double newZ) {
	x = newX;
	y = newY;
	z = newZ;
    }

    //accessor methods
    public double getX() {
	return x;
    }

    public double getY() {
	return y;
    }

    public double getZ() {
	return z;
    }

    //mutator methods
    public void setX(double newX) {
	x = newX;
    }

    public void setY(double newY) {
	y = newY;
    }
    
    public void setZ(double newZ) {
	z = newZ;
    }
}