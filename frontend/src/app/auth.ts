import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface User {
  name?: string;
  email: string;
  password?: string;
}

export interface AuthResponse {
  message: string;
  email?: string;
  name?: string;
  role?: string;
}

export interface ProfileResponse {
  name: string;
  email: string;
  role?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080';

  readonly currentUser = signal<ProfileResponse | null>(this.getStoredUser());

  constructor(private http: HttpClient) {}

  private getStoredUser(): ProfileResponse | null {
    const email = localStorage.getItem('userEmail');
    const name = localStorage.getItem('userName') || '';
    const role = localStorage.getItem('userRole') || 'ROLE_USER';
    if (email) {
      return { name, email, role };
    }
    return null;
  }

  isLoggedIn(): boolean {
    return !!this.currentUser();
  }

  isAdmin(): boolean {
    const user = this.currentUser();
    return user?.role === 'ROLE_ADMIN';
  }

  register(user: { name: string; email: string; password: string; adminCode?: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/register`,
      user,
      {
        withCredentials: true
      }
    );
  }

  login(credentials: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/login`,
      credentials,
      {
        withCredentials: true
      }
    ).pipe(
      tap((res) => {
        const user: ProfileResponse = {
          name: res.name || '',
          email: res.email || credentials.email,
          role: res.role || 'ROLE_USER'
        };
        this.currentUser.set(user);
        localStorage.setItem('userEmail', user.email);
        localStorage.setItem('userName', user.name);
        localStorage.setItem('userRole', user.role || 'ROLE_USER');
        localStorage.setItem('isLoggedIn', 'true');
      })
    );
  }

  logout(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.apiUrl}/logout`,
      {},
      {
        withCredentials: true
      }
    ).pipe(
      tap({
        next: () => this.clearSession(),
        error: () => this.clearSession()
      })
    );
  }

  clearSession(): void {
    this.currentUser.set(null);
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userName');
    localStorage.removeItem('userRole');
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('token');
    localStorage.removeItem('mart_user_cart');
  }

  getProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(
      `${this.apiUrl}/profile`,
      {
        withCredentials: true
      }
    ).pipe(
      tap((profile) => {
        this.currentUser.set(profile);
        localStorage.setItem('userEmail', profile.email);
        localStorage.setItem('userName', profile.name);
        localStorage.setItem('isLoggedIn', 'true');
      })
    );
  }

  isAuthenticated(): boolean {
    return localStorage.getItem('isLoggedIn') === 'true' || !!localStorage.getItem('userEmail');
  }

  forgotPassword(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/forgot-password`,
      { email }
    );
  }

  validateResetToken(token: string): Observable<{ valid: boolean; message: string }> {
    return this.http.get<{ valid: boolean; message: string }>(
      `${this.apiUrl}/validate-reset-token`,
      { params: { token } }
    );
  }

  resetPassword(token: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/reset-password`,
      { token, newPassword }
    );
  }
}