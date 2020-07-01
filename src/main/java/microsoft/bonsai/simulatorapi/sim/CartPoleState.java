package microsoft.bonsai.simulatorapi.sim;

public class CartPoleState  {

	
	public double cart_position;
	public double cart_velocity;
	public double pole_angle;
	public double pole_angular_velocity;
	public double pole_center_position;
	public double pole_center_velocity;
	public double target_pole_position;

	public CartPoleState() {

	}
	
    public CartPoleState(CartPoleModel cp) {
    	cart_position = cp.cart_position;
		cart_velocity = cp.cart_velocity;
		pole_angle = cp.pole_angle;
		pole_angular_velocity = cp.pole_angle;
		pole_center_position = cp.pole_angle;
		pole_center_velocity = cp.pole_angle;
		target_pole_position = cp.pole_angle;
	}
}
