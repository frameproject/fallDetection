package com.demo.accelerometer;

/**
 * Projet GMM - INSA 2017
 *
 * @author : fcamps@laas.fr
 *
 */
public class Compute {
    // Matrice de rotation totale (identit� � l'initialisation
    public float[][] R = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    // Matrice de rotation entre 2 iterrations
    public float[][] current = new float[3][3];
    // Coefficient de poids pour la filtre
    public static float alpha;

    // Constructeur
    public Compute(float alph) {
        alpha = alph;
    }


    // Angle entre 2 vecteurs
    public float getAngle(float[] a0, float[] a1) {
        float angle;
        angle = (float) Math.acos(ps(a0, a1) / (norme(a0) * norme(a1)));
        return (float) (angle * 180 / Math.PI);
    }

    // Quaternion des valeurs du gyrometre
    public float[] quaternion(float[] gyroData, float dt) {
        float[] quat = new float[4];
        float theta;
        float n;
        n = norme(gyroData);
        theta = dt * n;
        quat[0] = (float) Math.cos(theta / 2f);
        quat[1] = (float) Math.sin(theta / 2f) * gyroData[0] / n;
        quat[2] = (float) Math.sin(theta / 2f) * gyroData[1] / n;
        quat[3] = (float) Math.sin(theta / 2f) * gyroData[2] / n;
        return quat;
    }

    // Matrice de rotation a partir du quaternion
    public void getMatrixFromQuaternion(float[] quat) {
        current[0][0] = quat[0] * quat[0] + quat[1] * quat[1] - quat[2] * quat[2] - quat[3] * quat[3];
        current[1][0] = 2 * quat[0] * quat[3] + 2 * quat[1] * quat[2];
        current[0][1] = 2 * quat[1] * quat[2] - 2 * quat[0] * quat[3];
        current[1][1] = quat[0] * quat[0] - quat[1] * quat[1] + quat[2] * quat[2] - quat[3] * quat[3];
        current[2][0] = 2 * quat[1] * quat[3] - 2 * quat[0] * quat[2];
        current[0][2] = 2 * quat[1] * quat[3] + 2 * quat[0] * quat[2];
        current[2][2] = quat[0] * quat[0] - quat[1] * quat[1] - quat[2] * quat[2] + quat[3] * quat[3];
        current[1][2] = 2 * quat[2] * quat[3] - 2 * quat[0] * quat[1];
        current[2][1] = 2 * quat[2] * quat[3] + 2 * quat[0] * quat[1];
    }

    // Matrice totale
    public void computeR() {
        R = multiplyMat(R, current);
    }

    // Angle par gyrometre
    public float angleGyro(float[] init, float[] a0, float dt) {
        float[] a = new float[3];
        float[] q = new float[4];
        q = quaternion(a0, dt);
        getMatrixFromQuaternion(q);
        computeR();
        a = multiplyMV(R, init);
        return getAngle(init, a);
    }

    // Angle avec filtre complementaire
    public float angleFilter(float angleA, float angleG) {
        return alpha * angleG + (1 - alpha) * angleA;
    }

    /************************************************
     * OPERATIONS SUR LES VECTEURS ET MATRICES
     ***********************************************/

    // Multiplication entre 2 matrices
    public static float[][] multiplyMat(float[][] a, float[][] b) {
        int m1 = a.length;
        int n1 = a[0].length;
        int m2 = b.length;
        int n2 = b[0].length;
        if (n1 != m2) throw new RuntimeException("Illegal matrix dimensions.");
        float[][] c = new float[m1][n2];
        for (int i = 0; i < m1; i++)
            for (int j = 0; j < n2; j++)
                for (int k = 0; k < n1; k++)
                    c[i][j] += a[i][k] * b[k][j];
        return c;
    }

    // Multiplication matrice-vecteur
    public static float[] multiplyMV(float[][] a, float[] x) {
        int m = a.length;
        int n = a[0].length;
        if (x.length != n) throw new RuntimeException("Illegal matrix dimensions.");
        float[] y = new float[m];
        for (int i = 0; i < m; i++)
            for (int j = 0; j < n; j++)
                y[i] += a[i][j] * x[j];
        return y;
    }

    // Norme d un vecteur
    public static float norme(float[] v) {
        float n;
        float s = 0;
        int L = v.length;
        for (int i = 0; i < L; i++) {
            s = s + v[i] * v[i];
        }
        n = (float) Math.sqrt(s);
        return n;
    }

    // Produit scalaire entre 2 vecteurs
    public static float ps(float[] v1, float[] v2) {
        int m = v1.length;
        int n = v2.length;
        if (m != n) throw new RuntimeException("Illegal vector dimensions.");
        float ps = 0;
        for (int i = 0; i < m; i++) {
            ps += v1[i] * v2[i];
        }
        return ps;
    }

}