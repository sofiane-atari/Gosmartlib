import { computed, inject, Injectable } from '@angular/core';
import { AuthService } from '../authorization/auth.service';
import { Role } from '../../model/auth.model';

/**
 * Service responsible for role-based authorization in the frontend.
 *
 * This service reads the current role of the logged-in user from the AuthService
 * and provides helper methods to check whether a user has a specific role
 * or permission.
 *
 * Note:
 * these checks are only intended for frontend rendering and usability,
 * such as showing or hiding buttons and page sections.
 * Real security must always be enforced on the backend.
 */

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  private readonly authService = inject(AuthService);

  /**
   * The current role of the logged-in user.
   *
   * Returns `null` when no user is logged in
   * or when the user information has not been loaded yet.
   */
  public readonly currentRole = computed<Role | null>(() => {
    return this.authService.currentUser()?.role ?? null;
  });

  /**
   * Checks whether the current user has exactly the specified role.
   *
   * @param role The role to check.
   * @returns `true` if the user has this role, otherwise `false`.
   */
  public hasRole(role: Role): boolean {
    return this.currentRole() === role;
  }

  /**
   * Checks whether the current user has at least one of the specified roles.
   *
   * This method is useful for permissions that are available
   * to multiple roles, such as administrative or staff functionality.
   *
   * @param roles List of allowed roles.
   * @returns `true` if the current user's role is included in the list, otherwise `false`.
   */
  public hasAnyRole(roles: Role[]): boolean {
    const currentRole = this.currentRole();

    if (!currentRole) {
      return false;
    }

    if (currentRole === 'ADMIN') {
      return true;
    }

    return roles.includes(currentRole);
  }

  public isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  public isLibrarian(): boolean {
    return this.hasRole('LIBRARIAN');
  }

  public isTeacher(): boolean {
    return this.hasRole('TEACHER');
  }

  public isStudent(): boolean {
    return this.hasRole('STUDENT');
  }

  /**
   * Determines whether the user is allowed to manage books.
   *
   * This permission is available to:
   * - administrators
   * - librarians
   * - teachers
   *
   * @returns `true` if the user is allowed to manage books, otherwise `false`.
   */
  public canManageBooks(): boolean {
    return this.hasAnyRole(['ADMIN', 'LIBRARIAN', 'TEACHER']);
  }

  /**
   * Too many methods for the same roles
   * refactor LATER //////////////////////////////////////////////////////////////////////////
   */
  public canManageLessonTip(): boolean {
    return this.hasAnyRole(['ADMIN', 'LIBRARIAN', 'TEACHER']);
  }

  /**
   * Determines whether the user is allowed to manage genres.
   *
   * Only administrators and librarians are allowed to modify genres.
   *
   * @returns `true` if the user is allowed to manage genres, otherwise `false`.
   */
  public canManageGenres(): boolean {
    return this.hasAnyRole(['ADMIN', 'LIBRARIAN']);
  }

  /**
   * Determines whether the user is allowed to perform a bulk import of books.
   *
   * Only administrators and librarians have access
   * to this management functionality.
   *
   * @returns `true` if the user is allowed to perform bulk import, otherwise `false`.
   */
  public canManageBulkImport(): boolean {
    return this.hasAnyRole(['ADMIN', 'LIBRARIAN']);
  }

  /**
   * Determines whether the user is allowed to moderate reviews.
   *
   * Only administrators and librarians are allowed to manage or delete reviews.
   *
   * @returns `true` if the user is allowed to moderate reviews, otherwise `false`.
   */
  public canModerateReviews(): boolean {
    return this.hasAnyRole(['ADMIN', 'LIBRARIAN']);
  }

  /**
   * Determines whether the user has access to features
   * that are intended exclusively for administrators.
   *
   * @returns `true` if the user is an administrator, otherwise `false`.
   */
  public canAccessAdminOnlyFeatures(): boolean {
    return this.hasRole('ADMIN');
  }

  public canModifyLessonTip(ownerUserId: string | null | undefined): boolean {
    const currentUserId = this.authService.currentUser()?.id;

    if (this.isAdmin()) {
      return true;
    }

    if (!currentUserId || !ownerUserId) {
      return false;
    }

    return (
      currentUserId === ownerUserId && this.hasAnyRole(['TEACHER', 'LIBRARIAN'])
    );
  }
}
