package org.jlab.clas.detector;

import java.util.ArrayList;
import java.util.List;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Plane3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;


/**
 *
 * @author baltzell
 */
public class CherenkovResponse extends DetectorResponse {

    public class TrackResidual {
        private double coneAngle;
        private double dTheta;
        private double dPhi;
        public double getDeltaTheta() { return this.dTheta; }
        public double getDeltaPhi()   { return this.dPhi; }
        public double getConeAngle()  { return this.coneAngle; }
        public TrackResidual(CherenkovResponse cher, Line3D track) {
            Point3D intersection = cher.getIntersection(track);
            Vector3D vecTrk = intersection.toVector3D();
            Vector3D vecHit = cher.hitPosition.toVector3D();
            this.dTheta = vecHit.theta()-vecTrk.theta();
            this.dPhi = cher.getDeltaPhi(vecHit.phi(),vecTrk.phi());
            this.coneAngle = vecHit.angle(vecTrk);
        }
    }

    // theta/phi resolution for this response:
    private double  hitDeltaPhi   = 0.0;
    private double  hitDeltaTheta = 0.0;

    // FIXME:  remove this, use hitPosition instead:
    private double  hitTheta      = 0.0;
    private double  hitPhi        = 0.0;

    // FIXME:  remove this, use DetectorResponse's hitPosition instead:
    private Point3D hitPosition = new Point3D();

    public CherenkovResponse(double theta, double phi, double dtheta, double dphi){
        hitTheta = theta;
        hitPhi   = phi;
        hitDeltaTheta  = dtheta;
        hitDeltaPhi    = dphi;
    }

    public double getNphe() {return this.getEnergy(); }
    public double getTheta() {return this.hitTheta;}
    public double getPhi() {return this.hitPhi;}
    public double getDeltaTheta(){ return this.hitDeltaTheta;}
    public double getDeltaPhi() {return this.hitDeltaPhi;}

    public Point3D getHitPosition(){
        return this.hitPosition;
    }

    public void setHitPosition(double x, double y, double z){
        this.hitPosition.set(x, y, z);
    }

    /**
     * Wrap delta-phi into -pi/pi:
     */
    public double getDeltaPhi(double phi1, double phi2) {
        return Math.IEEEremainder(phi1-phi2,2.*Math.PI);
    }

    public Point3D getIntersection(Line3D line){
        Vector3D vec = new Vector3D(this.hitPosition.x(),this.hitPosition.y(),this.hitPosition.z());
        vec.unit();        
        Plane3D plane = new Plane3D(this.hitPosition,vec);
        Point3D intersect = new Point3D();
        plane.intersection(line, intersect);
        return intersect;
    }

    public TrackResidual getTrackResidual(Line3D particleTrack) {
        return new TrackResidual(this,particleTrack);
    }
  
    public static List<DetectorResponse>  readHipoEvent(DataEvent event, 
            String bankName, DetectorType type){        
        List<DetectorResponse> responseList = new ArrayList<DetectorResponse>();
        if(event.hasBank(bankName)==true){
            DataBank bank = event.getBank(bankName);
            int nrows = bank.rows();
            for(int row = 0; row < nrows; row++){

                double x = bank.getFloat("x",row);
                double y = bank.getFloat("y",row);
                double z = bank.getFloat("z",row);
                double time = bank.getFloat("time",row);
                double nphe = bank.getFloat("nphe", row);

                // FIXME: unify LTCC/HTCC detector banks
                // Here we have to treat them differently:
                // 2.  either add dtheta/dphi to LTCC, or ignore HTCC's and use CCDB for both
                // 3.  HTCC provides both x/y/z and theta/phi, while LTCC provides only x/y/z.
                //     The current convention in EB is to use only x/y/z, while theta/phi is
                //     just propogated.

                double theta=0,phi=0,dtheta=0,dphi=0;

                if (type==DetectorType.HTCC) {
                    dtheta = bank.getFloat("dtheta",row);
                    dphi = bank.getFloat("dphi",row);
                    theta = bank.getFloat("theta", row);
                    phi = bank.getFloat("phi",row);
                }

                else if (type==DetectorType.LTCC) {
                    // Hardcode some dtheta/dphi values for now (for matching):
                    dtheta = 10*3.14159/180;
                    dphi   = 10*3.14159/180;
                    // Fill theta/phi:
                    // Not yet.
                }

                else {
                    throw new RuntimeException(
                            "CherenkovResponse::readHipoEvent:  invalid DetectorType: "+type);
                }

                CherenkovResponse che = new CherenkovResponse(theta,phi,dtheta,dphi);
                che.setHitPosition(x, y, z);
                che.setHitIndex(row);
                che.setEnergy(nphe);
                che.setTime(time);
                che.getDescriptor().setType(type);

                responseList.add((DetectorResponse)che);
            }
        }
        return responseList;
    }

}

