import { animate, animation, style, transition, trigger, useAnimation } from '@angular/animations';

const enterAnimation = animation([
  style({
    opacity: 0,
    transform: 'translateY(28px) scale(0.97)',
  }),
  animate(
    '{{delay}}ms ease-out',
    style({
      opacity: 0,
      transform: 'translateY(28px) scale(0.97)',
    }),
  ),
  animate(
    '650ms cubic-bezier(0.22, 1, 0.36, 1)',
    style({
      opacity: 1,
      transform: 'translateY(0) scale(1)',
    }),
  ),
]);

export const reviewCardAnimation = trigger('reviewCardAnimation', [
  transition(':enter', [
    useAnimation(enterAnimation, {
      params: { delay: 0 },
    }),
  ]),
  transition(':leave', [
    animate(
      '320ms cubic-bezier(0.4, 0, 0.6, 1)',
      style({
        opacity: 0,
        transform: 'translateX(-20px) scale(0.95)',
        height: 0,
        marginBottom: 0,
        paddingTop: 0,
        paddingBottom: 0,
      }),
    ),
  ]),
]);

export const starHoverAnimation = trigger('starHover', [
  transition('* => hover', [
    animate(
      '200ms cubic-bezier(0.34, 1.56, 0.64, 1)',
      style({
        transform: 'scale(1.15) rotate(12deg)',
      }),
    ),
  ]),
  transition('hover => *', [
    animate(
      '200ms ease-out',
      style({
        transform: 'scale(1) rotate(0deg)',
      }),
    ),
  ]),
]);

export const buttonRippleAnimation = trigger('buttonRipple', [
  transition('* => clicked', [
    style({
      transform: 'scale(0.95)',
    }),
    animate(
      '150ms cubic-bezier(0.4, 0, 0.2, 1)',
      style({
        transform: 'scale(1)',
      }),
    ),
  ]),
]);

export const fadeInUpAnimation = trigger('fadeInUp', [
  transition(':enter', [
    style({
      opacity: 0,
      transform: 'translateY(20px)',
    }),
    animate(
      '500ms cubic-bezier(0.22, 1, 0.36, 1)',
      style({
        opacity: 1,
        transform: 'translateY(0)',
      }),
    ),
  ]),
]);

export const shimmerAnimation = trigger('shimmer', [
  transition(':enter', [
    style({
      backgroundPosition: '-200% center',
    }),
    animate(
      '2000ms ease-in-out',
      style({
        backgroundPosition: '200% center',
      }),
    ),
  ]),
]);
