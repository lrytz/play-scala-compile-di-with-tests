package controllers;

import play.mvc.*;

public class JavaController extends Controller {
    public Result hi() { return ok("hi"); }
}
