package org.fruct.oss.socialnavigator.utils.quadtree;

public class Node {

    private double x;
    private double y;
    private double w;
    private double h;
    private Node opt_parent;
    private Point point;
    private NodeType nodetype = NodeType.EMPTY;
    private Node nw;
    private Node ne;
    private Node sw;
    private Node se;
/*
    private double aLat;
    private double aLon;
    private double bLat;
    private double bLon;*/
    private boolean used = false;
    private boolean used2 = false;

  /*  public void setMaster(double aLat, double aLon, double bLat, double bLon)
    {
        this.aLat = aLat;
        this.aLon = aLon;
        this.bLat = bLat;
        this.bLon = bLon;
    }*/
  /*  public boolean compareMaster(double aLat, double aLon, double bLat, double bLon)
    {
        if(this.aLat != aLat)
        {
            return false;
        }
        if(this.aLon != aLon)
        {
            return false;
        }
        if(this.bLat != bLat)
        {
            return false;
        }
        if(this.bLon != bLon)
        {
            return false;
        }
        return true;
    }*/
   /* public double getaLat()
    {
        return this.aLat;
    }
    public double getaLon()
    {
        return this.aLon;
    }
    public double getbLat()
    {
        return this.bLat;
    }
    public double getbLon()
    {
        return this.bLon;
    }*/

    /**
     * Constructs a new quad tree node.
     *
     * @param {double} x X-coordiate of node.
     * @param {double} y Y-coordinate of node.
     * @param {double} w Width of node.
     * @param {double} h Height of node.
     * @param {Node}   opt_parent Optional parent node.
     * @constructor
     */
    public Node(double x, double y, double w, double h, Node opt_parent) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.opt_parent = opt_parent;
    }

    public boolean getUsed(){
        return used;
    }
    public void setUsed(boolean used){
        this.used = used;
    }

    public boolean getUsed2(){
        return used2;
    }
    public void setUsed2(boolean used){
        this.used2 = used;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public Node getParent() {
        return opt_parent;
    }

    public void setParent(Node opt_parent) {
        this.opt_parent = opt_parent;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public Point getPoint() {
        return this.point;
    }

    public void setNodeType(NodeType nodetype) {
        this.nodetype = nodetype;
    }

    public NodeType getNodeType() {
        return this.nodetype;
    }


    public void setNw(Node nw) {
        this.nw = nw;
    }

    public void setNe(Node ne) {
        this.ne = ne;
    }

    public void setSw(Node sw) {
        this.sw = sw;
    }

    public void setSe(Node se) {
        this.se = se;
    }

    public Node getNe() {
        return ne;
    }

    public Node getNw() {
        return nw;
    }

    public Node getSw() {
        return sw;
    }

    public Node getSe() {
        return se;
    }
}
