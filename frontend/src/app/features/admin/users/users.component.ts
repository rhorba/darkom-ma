import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';

import { AdminUser } from '../../../core/admin/admin-user.model';
import { AdminUserService } from '../../../core/admin/admin-user.service';

@Component({
  selector: 'app-admin-users',
  imports: [MatButtonModule, MatTableModule, MatToolbarModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  private readonly adminUserService = inject(AdminUserService);
  private readonly snackBar = inject(MatSnackBar);

  readonly users = signal<AdminUser[]>([]);
  readonly displayedColumns = ['email', 'fullName', 'role', 'active', 'actions'];

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.adminUserService.list().subscribe((users) => this.users.set(users));
  }

  deactivate(user: AdminUser): void {
    this.adminUserService.deactivate(user.id).subscribe(() => {
      this.snackBar.open('Compte désactivé', 'OK', { duration: 3000 });
      this.reload();
    });
  }
}
