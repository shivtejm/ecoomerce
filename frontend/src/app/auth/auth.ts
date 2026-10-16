import { ChangeDetectorRef, Component, NgZone } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './auth.html',
  styleUrl: './auth.css'
})
export class Auth {

  // Login/Register tab
  isLogin = true;
  isForgot = false;

  // Login
  loginEmail = '';
  loginPassword = '';

  // Forgot password
  forgotEmail = '';

  // Register
  registerName = '';
  registerEmail = '';
  registerPassword = '';
  registerAdminCode = '';

  // Messages
  errorMessage = '';
  successMessage = '';

  // Loading state
  isLoading = false;

  private emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  // =====================================
  // SHOW LOGIN
  // =====================================
  showLogin(): void {
    this.isLogin = true;
    this.isForgot = false;
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = false;
  }

  // =====================================
  // SHOW REGISTER
  // =====================================
  showRegister(): void {
    this.isLogin = false;
    this.isForgot = false;
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = false;
  }

  // =====================================
  // SHOW FORGOT PASSWORD
  // =====================================
  showForgotPassword(): void {
    this.isForgot = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = false;
    this.forgotEmail = this.loginEmail ? this.loginEmail.trim() : '';
  }

  // =====================================
  // LOGIN
  // =====================================
  login(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const email = this.loginEmail.trim();
    const password = this.loginPassword;

    if (!email || !password) {
      this.errorMessage = 'Please enter both email and password.';
      return;
    }

    if (!this.emailRegex.test(email)) {
      this.errorMessage = 'Please enter a valid email address.';
      return;
    }

    this.isLoading = true;
    this.cdr.detectChanges();

    this.authService.login({ email, password }).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
          this.router.navigate(['/home/dashboard']);
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;

          if (error.status === 401) {
            this.errorMessage = 'Invalid email or password.';
          } else if (error.status === 0) {
            this.errorMessage = 'Cannot connect to backend. Make sure Spring Boot is running on port 8080.';
          } else if (error.status === 400) {
            this.errorMessage = error.error?.message || 'Invalid login details.';
          } else {
            this.errorMessage = error.error?.message || 'Login failed. Status: ' + error.status;
          }
          this.cdr.detectChanges();
        });
      }
    });
  }

  // =====================================
  // REGISTER
  // =====================================
  register(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const name = this.registerName.trim();
    const email = this.registerEmail.trim();
    const password = this.registerPassword;

    // Validate name
    if (!name) {
      this.errorMessage = 'Name is required.';
      return;
    }

    if (name.length < 2) {
      this.errorMessage = 'Name must be at least 2 characters.';
      return;
    }

    // Validate email
    if (!email) {
      this.errorMessage = 'Email is required.';
      return;
    }

    if (!this.emailRegex.test(email)) {
      this.errorMessage = 'Please enter a valid email address.';
      return;
    }

    // Validate password
    if (password.length < 8) {
      this.errorMessage = 'Password must be at least 8 characters.';
      return;
    }

    this.isLoading = true;
    this.cdr.detectChanges();

    const adminCode = this.registerAdminCode.trim();
    this.authService.register({ name, email, password, adminCode: adminCode ? adminCode : undefined }).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.successMessage = adminCode === 'ADMIN2026'
            ? 'Administrator account created successfully! Please login.'
            : 'Registration successful! Please login.';

          // Switch to login tab and pre-fill email
          this.isLogin = true;
          this.loginEmail = email;
          this.loginPassword = '';

          // Clear registration form
          this.registerName = '';
          this.registerEmail = '';
          this.registerPassword = '';
          this.registerAdminCode = '';
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;

          if (error.status === 409) {
            this.errorMessage = 'This email is already registered. Please login instead.';
          } else if (error.status === 400) {
            this.errorMessage = error.error?.message || 'Invalid registration data. Check your inputs.';
          } else if (error.status === 0) {
            this.errorMessage = 'Cannot connect to backend. Make sure Spring Boot is running on port 8080.';
          } else {
            this.errorMessage = error.error?.message || 'Registration failed. Status: ' + error.status;
          }
          this.cdr.detectChanges();
        });
      }
    });
  }

  // =====================================
  // FORGOT PASSWORD
  // =====================================
  forgotPassword(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const email = this.forgotEmail.trim();

    if (!email) {
      this.errorMessage = 'Please enter your email address.';
      return;
    }

    if (!this.emailRegex.test(email)) {
      this.errorMessage = 'Please enter a valid email address.';
      return;
    }

    this.isLoading = true;
    this.cdr.detectChanges();

    this.authService.forgotPassword(email).subscribe({
      next: (response) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          this.successMessage = response?.message || 'If this email is registered, a password reset link has been sent.';
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          if (error.status === 0) {
            this.errorMessage = 'Cannot connect to backend. Make sure Spring Boot is running on port 8080.';
          } else {
            this.errorMessage = error.error?.message || 'Failed to process request. Please try again.';
          }
          this.cdr.detectChanges();
        });
      }
    });
  }
}