import { ChangeDetectorRef, Component, OnInit, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../auth';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {

  token: string | null = null;
  tokenStatus: 'validating' | 'valid' | 'invalid' = 'validating';
  statusMessage = '';

  newPassword = '';
  confirmPassword = '';
  showNewPassword = false;
  showConfirmPassword = false;

  errorMessage = '';
  successMessage = '';
  isSubmitting = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');

    if (!this.token) {
      this.tokenStatus = 'invalid';
      this.statusMessage = 'Password reset link is missing or malformed. Please request a new reset link.';
      return;
    }

    this.authService.validateResetToken(this.token).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.tokenStatus = 'valid';
          this.cdr.detectChanges();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.tokenStatus = 'invalid';
          this.statusMessage = err?.error?.message || 'This password reset link is invalid or has expired.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  toggleShowNewPassword(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleShowConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.newPassword || !this.confirmPassword) {
      this.errorMessage = 'Please fill out both password fields.';
      return;
    }

    if (this.newPassword.length < 8) {
      this.errorMessage = 'Password must be at least 8 characters long.';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    if (!this.token) {
      this.errorMessage = 'Missing reset token.';
      return;
    }

    this.isSubmitting = true;
    this.cdr.detectChanges();

    this.authService.resetPassword(this.token, this.newPassword).subscribe({
      next: (res: { message?: string }) => {
        this.ngZone.run(() => {
          this.isSubmitting = false;
          this.successMessage = res?.message || 'Password reset successfully! Redirecting to login...';
          this.cdr.detectChanges();

          setTimeout(() => {
            this.router.navigate(['/auth']);
          }, 2000);
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          this.isSubmitting = false;
          this.errorMessage = err?.error?.message || 'Failed to reset password. Please try again.';
          this.cdr.detectChanges();
        });
      }
    });
  }
}
