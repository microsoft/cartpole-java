package microsoft.bonsai.samples.cartpole;

import java.util.Random;

public class Model {

	// simulation constants
	static double GRAVITY = 9.8; // a classic...
	static double CART_MASS = 0.31; // 1.0; // kg
	static double POLE_MASS = 0.055; // 0.1; // kg
	static double TOTAL_MASS = CART_MASS + POLE_MASS;
	static double POLE_HALF_LENGTH = 0.4 / 2; // 1.0/2.; half the pole's length in m
	static double POLE_MASS_LENGTH = POLE_MASS * POLE_HALF_LENGTH;
	static double FORCE_MAG = 1.0; // 10.0
	static double STEP_DURATION = 0.02; // seconds between state updates (20ms)
	static double TRACK_WIDTH = 1.0; // m
	static double FORCE_NOISE = 0.02; // % of FORCE_MAG

	//static double THETA_LIMIT = (12 * 2 * Math.PI) / 360; // 12deg in radians

	double x = 0; // cart position (m)
	double x_dot = 0; // cart velocity (m/s)
	//double theta = 0; // cart angle (rad)
	//double theta_dot = 0; // cart angular velocity (rad/s)
	double x_target = 0; // target pole position (m)
	double x_limit = TRACK_WIDTH / 2; // full track
	double x_delta = 0; // distance between target and pole position (m)
	double x_pole = 0; // pole position (m)
	double y_pole = 0; // height of pol COG (m)
	double x_pole_dot = 0; // pole velocity (m/s)

	double cart_position;
	double cart_velocity;
	double pole_angle;
	double pole_angular_velocity;
	double pole_center_position;
	double pole_center_velocity;
	double target_pole_position;

	public Model() {

	}

	public void reset()
	{
		cart_position = 0;
		cart_velocity = 0;
		pole_angle = 0;
		pole_angular_velocity = 0;
		pole_center_position = 0;
		target_pole_position = 0;
	}

	public void step(Action action) {
		double command = action.command;

		double min = -0.2;
		double max = 0.2;

		double rand = min + new Random().nextDouble() * (max-min);

		// simulation for a cart and a pole
		double force = FORCE_MAG * (command + rand); //command == 1 ? FORCE_MAG : -FORCE_MAG;
		double cosTheta = Math.cos(pole_angle);
		double sinTheta = Math.sin(pole_angle);

		double temp = (force + POLE_MASS_LENGTH * Math.pow(pole_angular_velocity, 2) * sinTheta) / TOTAL_MASS;
		double angularAcceleration = (GRAVITY * sinTheta - cosTheta * temp)
				/ (POLE_HALF_LENGTH * (4.0 / 3.0 - (POLE_MASS * Math.pow(cosTheta, 2)) / TOTAL_MASS));
		
		double linearAcceleration = temp - (POLE_MASS_LENGTH * angularAcceleration * cosTheta) / TOTAL_MASS;
		
		cart_position = cart_position + STEP_DURATION * cart_velocity;
		cart_velocity = cart_velocity + STEP_DURATION * linearAcceleration;
		
		pole_angle = pole_angle + STEP_DURATION * pole_angular_velocity;
		pole_angular_velocity = pole_angular_velocity + STEP_DURATION * angularAcceleration;

		//use the pole center, not the cart center, for tracking
		pole_center_position = cart_position + Math.sin(pole_angle) * POLE_HALF_LENGTH;
		pole_center_velocity = cart_velocity + Math.sin(pole_angular_velocity) * POLE_HALF_LENGTH;
	}

	public boolean halted() {
		return Math.abs(pole_angle) >= Math.PI/2;
	}


	public State getState() {
		return new State(this);
	}


}
