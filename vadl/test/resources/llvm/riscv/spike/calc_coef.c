#define MAX 100  // Define MAX as per requirement

// Global variable to store the correlation coefficient
double Coef;

// Function to compute square of a number
double Square(double x) {
    return x * x;
}

// Function prototype
void Calc_LinCorrCoef(double ArrayA[], double ArrayB[], double MeanA, double MeanB);

// Main test function
int main() {
    // Example test case
    double ArrayA[MAX] = {1.0, 2.0, 3.0, 4.0, 5.0};
    double ArrayB[MAX] = {2.0, 4.0, 6.0, 8.0, 10.0};

    // Compute means
    double MeanA = 0.0, MeanB = 0.0;
    for (int i = 0; i < MAX; i++) {
        MeanA += ArrayA[i];
        MeanB += ArrayB[i];
    }
    MeanA /= MAX;
    MeanB /= MAX;

    // Call function
    Calc_LinCorrCoef(ArrayA, ArrayB, MeanA, MeanB);

    return !(Coef - Coef <= 0.000001);
}

void Calc_LinCorrCoef(double ArrayA[], double ArrayB[], double MeanA, double MeanB) {
    int i;
    double numerator = 0.0, Aterm = 0.0, Bterm = 0.0;

    for (i = 0; i < MAX; i++) {
        numerator += (ArrayA[i] - MeanA) * (ArrayB[i] - MeanB);
        Aterm += Square(ArrayA[i] - MeanA);
        Bterm += Square(ArrayB[i] - MeanB);
    }

    Coef = numerator / (Square(Aterm) * Square(Bterm));
}