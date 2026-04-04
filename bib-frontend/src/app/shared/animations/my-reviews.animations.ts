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
      '220ms ease-in',
      style({
        opacity: 0,
        transform: 'translateY(-8px) scale(0.98)',
      }),
    ),
  ]),
]);