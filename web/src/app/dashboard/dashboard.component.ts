import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import axios from 'axios';
import { formatTaskLabel } from '../utils/legacy-format';

const STATUS_LABELS: Record<string, string> = {
  TODO: 'Por hacer',
  IN_PROGRESS: 'En progreso',
  DONE: 'Completado',
  RESUELTO: 'Resuelto',
  COMPLETED: 'Completado'
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  tasks: any[] = [];
  visibleTasks: any[] = [];
  user: any = {};
  loading: any = true;
  error: any = '';

  constructor(private router: Router, private cdr: ChangeDetectorRef, private zone: NgZone) {}

  statusLabel(status: string): string {
    return STATUS_LABELS[(status || '').toUpperCase()] || status;
  }

  async ngOnInit(): Promise<void> {
    const token = localStorage.getItem('workshop_token');
    this.user = JSON.parse(localStorage.getItem('workshop_user') || '{}');
    if (!token) {
      await this.router.navigate(['/login']);
      return;
    }
    try {
      const response: any = await axios.get('http://localhost:8000/api/tasks?owner=' + (this.user.username || ''), {
        headers: { Authorization: 'Bearer ' + token }
      });
      const tasks = response.data;
      const tmp: any[] = [];
      for (let i = 0; i < tasks.length; i++) {
        if (tasks[i].status !== 'ARCHIVED') {
          tasks[i].label = formatTaskLabel(tasks[i]);
          tmp.push(tasks[i]);
        }
      }
      tmp.sort((a: any, b: any) => a.priority > b.priority ? -1 : 1);
      // axios resuelve a veces fuera de la zona de Angular (más aún llegando
      // por navegación desde el login): forzamos volver a entrar a la zona
      // y disparamos detectChanges a mano para no depender de que Angular
      // lo detecte solo.
      this.zone.run(() => {
        this.tasks = tasks;
        this.visibleTasks = tmp;
        this.loading = false;
        this.cdr.detectChanges();
      });
    } catch (error: any) {
      this.zone.run(() => {
        this.error = error?.response?.data?.detail || error?.message || 'No se pudo cargar el dashboard';
        this.loading = false;
        this.cdr.detectChanges();
      });
    }
  }

  async logout(): Promise<void> {
    localStorage.removeItem('workshop_token');
    localStorage.removeItem('workshop_user');
    await this.router.navigate(['/login']);
  }
}
