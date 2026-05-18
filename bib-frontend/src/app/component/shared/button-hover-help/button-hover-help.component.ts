import { Component, Input, ElementRef, HostListener, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserSettingsService } from '../../../services/user-settings/user-settings.service';

@Component({
  selector: 'app-button-hover-help',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './button-hover-help.component.html',
  styleUrls: ['./button-hover-help.component.css']
})
export class ButtonHoverHelpComponent {
  @Input() text: string = '';
  @ViewChild('tooltipEl', { read: ElementRef }) tooltipEl!: ElementRef<HTMLElement>;

  isVisible = false;
  tooltipStyle: Record<string, string> = {};
  arrowStyle: Record<string, string> = {};

  constructor(
    public userSettingsService: UserSettingsService,
    private elementRef: ElementRef,
  ) {}

  @HostListener('mouseenter')
  onMouseEnter(): void {
    this.positionTooltip();
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    this.isVisible = false;
  }

  @HostListener('window:scroll')
  @HostListener('window:resize')
  onViewportChange(): void {
    if (this.isVisible) {
      this.positionTooltip();
    }
  }

  private positionTooltip(): void {
    const hostEl: HTMLElement = this.elementRef.nativeElement;
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const rect = hostEl.getBoundingClientRect();
    const gap = 10;
    const arrowSize = 10;
    const edgeThreshold = 1;

    const estHeight = 80;
    const estWidth = 200;
    const fitsAbove = rect.top - estHeight - gap - arrowSize > edgeThreshold;

    const top = fitsAbove
      ? rect.top - estHeight - gap - arrowSize
      : rect.bottom + gap + arrowSize;

    const centerX = rect.left + rect.width / 2;
    let tooltipLeft = centerX - estWidth / 2;
    tooltipLeft = Math.max(edgeThreshold, Math.min(tooltipLeft, vw - estWidth - edgeThreshold));

    let arrowLeft = '50%';
    if (tooltipLeft <= edgeThreshold) {
      const pct = ((centerX - tooltipLeft) / estWidth) * 100;
      arrowLeft = `${Math.max(pct, 10).toFixed(0)}%`;
    }

    this.tooltipStyle = {
      top: `${top}px`,
      left: `${tooltipLeft}px`,
    };

    this.arrowStyle = {
      left: arrowLeft,
      top: fitsAbove ? '100%' : '-5px',
      transform: fitsAbove
        ? 'translateX(-50%) translateY(-5px) rotate(45deg)'
        : 'translateX(-50%) rotate(-135deg)',
    };

    this.isVisible = true;

    requestAnimationFrame(() => this.refinePosition(rect, vw, vh, gap, arrowSize, edgeThreshold));
  }

  private refinePosition(
    rect: DOMRect, vw: number, vh: number,
    gap: number, arrowSize: number, edgeThreshold: number,
  ): void {
    if (!this.tooltipEl) return;

    const tooltip = this.tooltipEl.nativeElement;
    const tw = tooltip.offsetWidth;
    const th = tooltip.offsetHeight;

    const fitsAbove = rect.top - th - gap - arrowSize > edgeThreshold;

    let top: number;
    if (fitsAbove) {
      top = rect.top - th - gap - arrowSize;
    } else {
      top = rect.bottom + gap + arrowSize;
    }

    const centerX = rect.left + rect.width / 2;
    let tooltipLeft = centerX - tw / 2;

    let arrowLeft = '50%';
    if (tooltipLeft < edgeThreshold) {
      tooltipLeft = edgeThreshold;
      const pct = ((centerX - tooltipLeft) / tw) * 100;
      arrowLeft = `${Math.max(pct, 10).toFixed(0)}%`;
    } else if (tooltipLeft + tw > vw - edgeThreshold) {
      tooltipLeft = vw - tw - edgeThreshold;
      const pct = ((centerX - tooltipLeft) / tw) * 100;
      arrowLeft = `${Math.min(pct, 90).toFixed(0)}%`;
    }

    this.tooltipStyle = {
      top: `${top}px`,
      left: `${tooltipLeft}px`,
    };

    this.arrowStyle = {
      left: arrowLeft,
      top: fitsAbove ? '100%' : '-5px',
      transform: fitsAbove
        ? 'translateX(-50%) translateY(-5px) rotate(45deg)'
        : 'translateX(-50%) rotate(-135deg)',
    };
  }
}
