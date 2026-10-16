import {
  Component,
  OnInit,
  ChangeDetectorRef,
  NgZone
} from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../auth';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit {

  userName = '';
  userEmail = '';
  userRole = '';

  isLoading = true;
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.authService.getProfile().subscribe({
      next: (user) => {
        this.ngZone.run(() => {
          this.userName = user.name;
          this.userEmail = user.email;
          this.userRole = user.role || 'ROLE_USER';
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.isLoading = false;
          if (error.status === 401) {
            this.authService.clearSession();
            this.router.navigate(['/auth']);
            return;
          }
          this.errorMessage = 'Unable to load profile. Please try logging in again.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.ngZone.run(() => this.router.navigate(['/auth'])),
      error: () => this.ngZone.run(() => this.router.navigate(['/auth']))
    });
  }
}