/** Includes zero so negative equity is never drawn as positive. */
export function projectionGeometry(values: readonly number[]) {
  const minimum = Math.min(0, ...values);
  const maximum = Math.max(0, ...values);
  const span = maximum - minimum || 1;
  const y = (value: number) => 160 - ((value - minimum) / span) * 140;
  const points = values.map((value, index) => ({
    x: 20 + (index / Math.max(1, values.length - 1)) * 560,
    y: y(value),
  }));
  return { points, line: points.map(point => `${point.x},${point.y}`).join(' '), zeroY: y(0), minimum, maximum };
}
